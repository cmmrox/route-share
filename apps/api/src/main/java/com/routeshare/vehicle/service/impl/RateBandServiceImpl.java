package com.routeshare.vehicle.service.impl;

import com.routeshare.common.errors.GateCodes;
import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.vehicle.domain.RateBandCodes;
import com.routeshare.vehicle.domain.RatePosition;
import com.routeshare.vehicle.dto.request.RateBandAssessmentCommand;
import com.routeshare.vehicle.dto.request.RateBandReviewDecisionCommand;
import com.routeshare.vehicle.dto.request.RateBandReviewRequestCommand;
import com.routeshare.vehicle.dto.response.RateBandResponse;
import com.routeshare.vehicle.dto.response.RateBandReviewRequestResponse;
import com.routeshare.vehicle.dto.response.VehicleClassResponse;
import com.routeshare.vehicle.entity.RateBandReviewRequestEntity;
import com.routeshare.vehicle.entity.VehicleClassEntity;
import com.routeshare.vehicle.entity.VehicleEntity;
import com.routeshare.vehicle.entity.VehicleRateBandEntity;
import com.routeshare.vehicle.entity.VehicleRateBandFactorEntity;
import com.routeshare.vehicle.repository.RateBandReviewRequestRepository;
import com.routeshare.vehicle.repository.VehicleClassRepository;
import com.routeshare.vehicle.repository.VehicleRateBandFactorRepository;
import com.routeshare.vehicle.repository.VehicleRateBandRepository;
import com.routeshare.vehicle.repository.VehicleRepository;
import com.routeshare.vehicle.service.RateBandService;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Band assessment, rate choice and re-assessment.
 *
 * <p>The factor rows are checked against the offset from the class default and a mismatch is
 * <b>logged, not refused</b>: the displayed explanation must never be able to block an operational
 * price change. Getting that backwards would mean a band could not be corrected until its prose was
 * tidy.
 */
@Service
@Slf4j
public class RateBandServiceImpl implements RateBandService {
  private static final String COMIGO = "ComiGo";

  private final CurrentUserProvider current;
  private final IdentityFacade identity;
  private final DriverFacade drivers;
  private final VehicleRepository vehicles;
  private final VehicleClassRepository classes;
  private final VehicleRateBandRepository bands;
  private final VehicleRateBandFactorRepository factors;
  private final RateBandReviewRequestRepository reviews;
  private final NotificationFacade notifications;
  private final MeterRegistry meters;
  private final Clock clock;
  private final int reviewSlaDays;

  public RateBandServiceImpl(
      CurrentUserProvider current,
      IdentityFacade identity,
      DriverFacade drivers,
      VehicleRepository vehicles,
      VehicleClassRepository classes,
      VehicleRateBandRepository bands,
      VehicleRateBandFactorRepository factors,
      RateBandReviewRequestRepository reviews,
      NotificationFacade notifications,
      MeterRegistry meters,
      Clock clock,
      @Value("${routeshare.rate-band.review-sla-days:3}") int reviewSlaDays) {
    this.current = current;
    this.identity = identity;
    this.drivers = drivers;
    this.vehicles = vehicles;
    this.classes = classes;
    this.bands = bands;
    this.factors = factors;
    this.reviews = reviews;
    this.notifications = notifications;
    this.meters = meters;
    this.clock = clock;
    this.reviewSlaDays = reviewSlaDays;
    meters.gauge(
        "routeshare_rate_band_pending_assessment",
        this,
        // A rising gauge is drivers silently unable to earn: approved papers, no price.
        self -> self.bands.countByStatus(VehicleRateBandEntity.STATUS_PENDING_ASSESSMENT));
  }

  @Override
  @Transactional(readOnly = true)
  public List<VehicleClassResponse> vehicleClasses() {
    return classes.findByActiveTrueOrderBySortOrderAsc().stream()
        .map(
            c ->
                new VehicleClassResponse(
                    c.getClassKey(),
                    c.getLabel(),
                    c.getMaxPassengerSeats(),
                    c.getDefaultRateMin(),
                    c.getDefaultRateMax()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public RateBandResponse myBand(long vehicleId) {
    return toResponse(requireOwnedVehicle(vehicleId));
  }

  @Override
  @Transactional(readOnly = true)
  public RateBandResponse bandFor(long vehicleId) {
    return toResponse(requireVehicle(vehicleId));
  }

  @Override
  @Transactional
  public RateBandResponse chooseRate(long vehicleId, BigDecimal ratePerKm) {
    VehicleEntity vehicle = requireOwnedVehicle(vehicleId);
    VehicleRateBandEntity band = requireBand(vehicleId);
    if (!band.isActive()) {
      throw new GateConflictException(
          GateCodes.RATE_BAND_NOT_SET,
          "ComiGo is still setting the rate for this vehicle.",
          "/driver/vehicles/" + vehicleId + "/rate-band");
    }
    if (ratePerKm.compareTo(band.getRateMin()) < 0 || ratePerKm.compareTo(band.getRateMax()) > 0) {
      throw new GateConflictException(
          RateBandCodes.RATE_OUTSIDE_BAND,
          "Choose a rate between LKR %s and LKR %s per km."
              .formatted(plain(band.getRateMin()), plain(band.getRateMax())),
          "/driver/vehicles/" + vehicleId + "/rate-band");
    }
    BigDecimal previous = band.getChosenRate();
    band.setChosenRate(ratePerKm);
    VehicleRateBandEntity saved = bands.save(band);
    // A rate change is a price change; it is traceable at INFO, not only in an admin table.
    log.info(
        "rate band chosen-rate changed vehicleId={} from={} to={} driverProfileId={}",
        vehicleId,
        previous,
        ratePerKm,
        vehicle.getDriverProfileId());
    return toResponse(vehicle, saved);
  }

  @Override
  @Transactional
  public RateBandReviewRequestResponse requestReview(
      long vehicleId, RateBandReviewRequestCommand cmd) {
    requireOwnedVehicle(vehicleId);
    VehicleRateBandEntity band = requireBand(vehicleId);
    if (reviews
        .findByVehicleIdAndStatus(vehicleId, RateBandReviewRequestEntity.STATUS_OPEN)
        .isPresent()) {
      throw new GateConflictException(
          RateBandCodes.RATE_REVIEW_ALREADY_OPEN,
          "You already have a rate review open for this vehicle.",
          "/driver/vehicles/" + vehicleId + "/rate-band");
    }
    var saved =
        reviews.save(
            RateBandReviewRequestEntity.open(
                vehicleId, currentAppUserId(), cmd.reason(), cmd.note()));
    // The band under review stays live and chargeable — D39 is explicit that asking is not losing.
    band.setStatus(VehicleRateBandEntity.STATUS_UNDER_REVIEW);
    bands.save(band);
    meters.counter("routeshare_rate_band_review_requests_total", "status", "OPEN").increment();
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RateBandReviewRequestResponse> myReviewRequests(long vehicleId) {
    requireOwnedVehicle(vehicleId);
    return reviews.findByVehicleIdOrderByIdDesc(vehicleId).stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<RateBandReviewRequestResponse> reviewRequests(String status) {
    String wanted =
        status == null || status.isBlank()
            ? RateBandReviewRequestEntity.STATUS_OPEN
            : status.toUpperCase(java.util.Locale.ROOT);
    return reviews.findByStatusOrderByIdDesc(wanted).stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional
  public RateBandResponse assess(
      long vehicleId, RateBandAssessmentCommand cmd, long actorAppUserId) {
    VehicleEntity vehicle = requireVehicle(vehicleId);
    VehicleClassEntity vehicleClass = requireClass(vehicle.getClassKey());
    validateBandWithinClass(cmd.rateMin(), cmd.rateMax(), vehicleClass);

    VehicleRateBandEntity band =
        bands
            .findByVehicleId(vehicleId)
            .orElseGet(
                () ->
                    VehicleRateBandEntity.pendingAssessment(
                        vehicleId,
                        vehicleClass.getDefaultRateMin(),
                        vehicleClass.getDefaultRateMax()));
    band.setRateMin(cmd.rateMin());
    band.setRateMax(cmd.rateMax());
    band.setStatus(VehicleRateBandEntity.STATUS_ACTIVE);
    band.setSetByAppUserId(actorAppUserId);
    band.setSetAt(clock.instant());
    // A rate the driver already chose is kept if it still fits; otherwise the midpoint, so the
    // vehicle is publishable the moment the band lands rather than after another driver action.
    BigDecimal chosen = band.getChosenRate();
    if (chosen == null
        || chosen.compareTo(cmd.rateMin()) < 0
        || chosen.compareTo(cmd.rateMax()) > 0) {
      band.setChosenRate(midpoint(cmd.rateMin(), cmd.rateMax()));
    }
    VehicleRateBandEntity saved = bands.save(band);
    replaceFactors(saved, cmd.factors(), vehicleClass);

    meters.counter("routeshare_rate_band_assessments_total").increment();
    log.info(
        "rate band assessed vehicleId={} band={}-{} chosen={} actor={}",
        vehicleId,
        cmd.rateMin(),
        cmd.rateMax(),
        saved.getChosenRate(),
        actorAppUserId);
    notifyDriver(
        vehicle,
        "RATE_BAND_SET",
        "Your rate is set",
        "ComiGo set LKR %s–%s per km for %s. You can pick your rate now."
            .formatted(plain(cmd.rateMin()), plain(cmd.rateMax()), label(vehicle)));
    return toResponse(vehicle, saved);
  }

  @Override
  @Transactional
  public RateBandReviewRequestResponse decideReview(
      long requestId, RateBandReviewDecisionCommand cmd, long actorAppUserId) {
    RateBandReviewRequestEntity request =
        reviews
            .findById(requestId)
            .orElseThrow(() -> new NoSuchElementException("Review request not found"));
    if (!RateBandReviewRequestEntity.STATUS_OPEN.equals(request.getStatus())) {
      throw new GateConflictException(
          RateBandCodes.RATE_REVIEW_ALREADY_OPEN,
          "This review has already been decided.",
          "/admin/rate-band-review-requests");
    }
    VehicleEntity vehicle = requireVehicle(request.getVehicleId());
    boolean approved = RateBandReviewRequestEntity.STATUS_APPROVED.equals(cmd.decision());

    if (approved && cmd.rateMin() != null && cmd.rateMax() != null) {
      assess(
          vehicle.getId(),
          new RateBandAssessmentCommand(cmd.rateMin(), cmd.rateMax(), cmd.factors(), cmd.note()),
          actorAppUserId);
    } else {
      // Rejected, or approved with no new numbers: the live band is simply released from review.
      bands
          .findByVehicleId(vehicle.getId())
          .ifPresent(
              band -> {
                if (VehicleRateBandEntity.STATUS_UNDER_REVIEW.equals(band.getStatus())) {
                  band.setStatus(VehicleRateBandEntity.STATUS_ACTIVE);
                  bands.save(band);
                }
              });
    }

    request.decide(
        approved
            ? RateBandReviewRequestEntity.STATUS_APPROVED
            : RateBandReviewRequestEntity.STATUS_REJECTED,
        clock.instant(),
        actorAppUserId,
        cmd.note());
    var saved = reviews.save(request);
    meters
        .counter("routeshare_rate_band_review_requests_total", "status", saved.getStatus())
        .increment();
    notifyDriver(
        vehicle,
        "RATE_BAND_REVIEW_DECIDED",
        approved ? "Your rate review was accepted" : "Your rate review was answered",
        cmd.note() == null ? "Open your rate band to see the outcome." : cmd.note());
    return toResponse(saved);
  }

  @Override
  @Transactional
  public void ensureBandExists(long vehicleId) {
    if (bands.findByVehicleId(vehicleId).isPresent()) {
      return;
    }
    VehicleEntity vehicle = requireVehicle(vehicleId);
    VehicleClassEntity vehicleClass = requireClass(vehicle.getClassKey());
    bands.save(
        VehicleRateBandEntity.pendingAssessment(
            vehicleId, vehicleClass.getDefaultRateMin(), vehicleClass.getDefaultRateMax()));
  }

  // ── internals ────────────────────────────────────────────────────────────────────────────────

  private void validateBandWithinClass(
      BigDecimal rateMin, BigDecimal rateMax, VehicleClassEntity vehicleClass) {
    if (rateMax.compareTo(rateMin) < 0) {
      throw new IllegalArgumentException("rateMax must be greater than or equal to rateMin");
    }
    if (rateMin.compareTo(vehicleClass.getDefaultRateMin()) < 0
        || rateMax.compareTo(vehicleClass.getDefaultRateMax()) > 0) {
      throw new GateConflictException(
          RateBandCodes.BAND_OUTSIDE_CLASS,
          "A %s band must sit between LKR %s and LKR %s per km."
              .formatted(
                  vehicleClass.getLabel(),
                  plain(vehicleClass.getDefaultRateMin()),
                  plain(vehicleClass.getDefaultRateMax())),
          "/admin/vehicles");
    }
  }

  private void replaceFactors(
      VehicleRateBandEntity band,
      List<RateBandAssessmentCommand.FactorCommand> commands,
      VehicleClassEntity vehicleClass) {
    factors.deleteByVehicleRateBandId(band.getId());
    if (commands == null || commands.isEmpty()) {
      return;
    }
    List<VehicleRateBandFactorEntity> rows = new ArrayList<>();
    int order = 0;
    for (var factor : commands) {
      rows.add(
          VehicleRateBandFactorEntity.of(
              band.getId(),
              factor.key().toUpperCase(java.util.Locale.ROOT),
              factor.label(),
              factor.detail(),
              factor.delta(),
              order++));
    }
    factors.saveAll(rows);

    BigDecimal declared =
        rows.stream()
            .map(VehicleRateBandFactorEntity::getDelta)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal actual =
        midpoint(band.getRateMin(), band.getRateMax())
            .subtract(midpoint(vehicleClass.getDefaultRateMin(), vehicleClass.getDefaultRateMax()));
    if (declared.subtract(actual).abs().compareTo(BigDecimal.ONE) > 0) {
      // Warn, never refuse: prose must not be able to block a price correction.
      log.warn(
          "rate band factors do not explain the band vehicleId={} declared={} actual={}",
          band.getVehicleId(),
          declared,
          actual);
    }
  }

  private RateBandResponse toResponse(VehicleEntity vehicle) {
    return toResponse(vehicle, bands.findByVehicleId(vehicle.getId()).orElse(null));
  }

  private RateBandResponse toResponse(VehicleEntity vehicle, VehicleRateBandEntity written) {
    VehicleClassEntity vehicleClass = requireClass(vehicle.getClassKey());
    VehicleRateBandEntity band =
        Optional.ofNullable(written)
            .orElseGet(
                () ->
                    VehicleRateBandEntity.pendingAssessment(
                        vehicle.getId(),
                        vehicleClass.getDefaultRateMin(),
                        vehicleClass.getDefaultRateMax()));

    List<RateBandResponse.Factor> factorRows =
        band.getId() == null
            ? List.of()
            : factors.findByVehicleRateBandIdOrderBySortOrderAsc(band.getId()).stream()
                .map(
                    f ->
                        new RateBandResponse.Factor(
                            f.getFactorKey(), f.getLabel(), f.getDetail(), f.getDelta()))
                .toList();
    BigDecimal netEffect =
        factorRows.stream()
            .map(RateBandResponse.Factor::delta)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    RatePosition position =
        RatePosition.of(band.getChosenRate(), band.getRateMin(), band.getRateMax());

    return new RateBandResponse(
        vehicle.getId(),
        label(vehicle),
        vehicleClass.getClassKey(),
        vehicleClass.getLabel(),
        new RateBandResponse.Range(
            vehicleClass.getDefaultRateMin(), vehicleClass.getDefaultRateMax()),
        new RateBandResponse.Range(band.getRateMin(), band.getRateMax()),
        band.getChosenRate(),
        band.getStatus(),
        // The assessing admin is never named to the driver; the platform owns the decision.
        band.getSetAt() == null ? null : COMIGO,
        band.getSetAt(),
        factorRows,
        netEffect,
        toPosition(position),
        List.of(
            toPosition(RatePosition.MIN),
            toPosition(RatePosition.MID),
            toPosition(RatePosition.MAX)),
        openReview(vehicle.getId()));
  }

  private RateBandResponse.Position toPosition(RatePosition position) {
    return new RateBandResponse.Position(
        position.name().toLowerCase(java.util.Locale.ROOT),
        position.label(),
        position.rank(),
        position.demand());
  }

  private RateBandResponse.ReviewRequest openReview(long vehicleId) {
    return reviews
        .findByVehicleIdAndStatus(vehicleId, RateBandReviewRequestEntity.STATUS_OPEN)
        .map(
            r ->
                new RateBandResponse.ReviewRequest(
                    r.getId(), r.getStatus(), r.getRequestedAt(), reviewSlaDays, null))
        .orElse(null);
  }

  private RateBandReviewRequestResponse toResponse(RateBandReviewRequestEntity entity) {
    return new RateBandReviewRequestResponse(
        entity.getId(),
        entity.getVehicleId(),
        entity.getReason(),
        entity.getNote(),
        entity.getStatus(),
        entity.getRequestedAt(),
        entity.getDecidedAt(),
        entity.getDecisionNote());
  }

  private void notifyDriver(VehicleEntity vehicle, String type, String title, String body) {
    try {
      drivers
          .findAppUserIdByDriverProfileId(vehicle.getDriverProfileId())
          .ifPresent(
              appUserId ->
                  notifications.notifyUser(
                      appUserId,
                      type,
                      title,
                      body,
                      Map.of("vehicleId", String.valueOf(vehicle.getId()))));
    } catch (RuntimeException ex) {
      // A band that is set but not announced is recoverable; a failed assessment is not.
      log.warn("rate band notification failed vehicleId={}", vehicle.getId(), ex);
    }
  }

  private VehicleEntity requireVehicle(long vehicleId) {
    return vehicles
        .findById(vehicleId)
        .orElseThrow(() -> new NoSuchElementException("Vehicle not found"));
  }

  private VehicleEntity requireOwnedVehicle(long vehicleId) {
    long driverProfileId = currentDriverProfileId();
    VehicleEntity vehicle = requireVehicle(vehicleId);
    if (!vehicle.getDriverProfileId().equals(driverProfileId)) {
      throw new AccessDeniedException("Vehicle belongs to another driver");
    }
    return vehicle;
  }

  private VehicleRateBandEntity requireBand(long vehicleId) {
    return bands
        .findByVehicleId(vehicleId)
        .orElseThrow(
            () ->
                new GateConflictException(
                    GateCodes.RATE_BAND_NOT_SET,
                    "ComiGo is still setting the rate for this vehicle.",
                    "/driver/vehicles/" + vehicleId + "/rate-band"));
  }

  private VehicleClassEntity requireClass(String classKey) {
    return classes
        .findById(classKey)
        .orElseThrow(() -> new NoSuchElementException("Unknown vehicle class: " + classKey));
  }

  private long currentAppUserId() {
    return identity.upsertFromToken(current.requireCurrentUser()).appUserId();
  }

  private long currentDriverProfileId() {
    return drivers
        .findDriverProfileIdByAppUserId(currentAppUserId())
        .orElseThrow(() -> new AccessDeniedException("Driver profile is required"));
  }

  private static BigDecimal midpoint(BigDecimal min, BigDecimal max) {
    return min.add(max).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
  }

  private static String plain(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }

  private static String label(VehicleEntity vehicle) {
    return vehicle.getMake() + " · " + vehicle.getRegistrationNumber();
  }
}
