package com.routeshare.routing.service.impl;

import com.routeshare.booking.facade.BookingFacade;
import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.event.DomainEvent;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.payment.facade.PaymentFacade;
import com.routeshare.penalty.facade.PenaltyFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.routing.domain.ApprovalMode;
import com.routeshare.routing.domain.TripEditability;
import com.routeshare.routing.dto.request.ApprovalModeRequest;
import com.routeshare.routing.dto.request.OccurrenceCancellationRequest;
import com.routeshare.routing.dto.response.AlternativeTripResponse;
import com.routeshare.routing.dto.response.OccurrenceCancellationTermsResponse;
import com.routeshare.routing.dto.response.OccurrenceEditabilityResponse;
import com.routeshare.routing.entity.RouteOccurrenceCancellationEntity;
import com.routeshare.routing.repository.RouteOccurrenceCancellationRepository;
import com.routeshare.routing.repository.RouteOccurrenceRepository;
import com.routeshare.routing.repository.RouteOccurrenceSeatRepository;
import com.routeshare.routing.service.OccurrenceLifecycleService;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OccurrenceLifecycleServiceImpl implements OccurrenceLifecycleService {
  private static final List<String> REASON_CODES =
      List.of("VEHICLE_PROBLEM", "UNWELL", "PLANS_CHANGED", "WRONG_DETAILS", "OTHER");
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final RouteOccurrenceRepository occurrences;
  private final RouteOccurrenceSeatRepository seats;
  private final RouteOccurrenceCancellationRepository cancellations;
  private final PolicySettingService policy;
  private final PenaltyFacade penalties;
  private final PaymentFacade payments;
  private final BookingFacade bookings;
  private final NotificationFacade notifications;
  private final DomainEventPublisher events;
  private final MeterRegistry meters;
  private final Clock clock;

  // ── approval mode ────────────────────────────────────────────────────────────────────────────

  @Override
  @Transactional
  public Map<String, Object> setApprovalMode(long routeOccurrenceId, ApprovalModeRequest request) {
    requireOwner(routeOccurrenceId);
    // Who may book is part of the deal the first rider already accepted, so it freezes with
    // everything else.
    requireEditable(routeOccurrenceId);
    ApprovalMode mode = ApprovalMode.valueOf(request.mode());
    occurrences.updateApprovalMode(routeOccurrenceId, mode.name());
    return Map.of("routeOccurrenceId", routeOccurrenceId, "approvalMode", mode.name());
  }

  // ── freeze ───────────────────────────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public OccurrenceEditabilityResponse editability(long routeOccurrenceId) {
    var context = context(routeOccurrenceId);
    int held = seats.countLiveHolds(routeOccurrenceId);
    boolean editable = TripEditability.isEditable(context.getStatus(), held);
    return new OccurrenceEditabilityResponse(
        routeOccurrenceId,
        editable,
        held,
        editable
            ? "Nobody has booked yet, so you can still change this trip."
            : TripEditability.freezeReason(held));
  }

  @Override
  @Transactional(readOnly = true)
  public void requireEditable(long routeOccurrenceId) {
    var editability = editability(routeOccurrenceId);
    if (!editability.editable()) {
      throw new GateConflictException(
          "TRIP_FROZEN", editability.reason(), "/driver/route-occurrences/" + routeOccurrenceId);
    }
  }

  // ── cancellation ─────────────────────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public OccurrenceCancellationTermsResponse cancellationTerms(long routeOccurrenceId) {
    requireOwner(routeOccurrenceId);
    var context = context(routeOccurrenceId);
    int freeHours = policy.integer(PolicyKey.DRIVER_CANCEL_FREE_HOURS);
    BigDecimal hoursOut = hoursUntil(context.getDepartsAt());
    boolean free = hoursOut.compareTo(BigDecimal.valueOf(freeHours)) >= 0;
    BigDecimal penaltyPct = policy.decimal(PolicyKey.LATE_CANCEL_PENALTY_PCT);

    var riders = occurrences.findAffectedRiders(routeOccurrenceId);
    var priced =
        bookings
            .findTripIdForOccurrence(routeOccurrenceId)
            .map(tripId -> penalties.priceOccurrenceCancellation(tripId, penaltyPct))
            .orElse(new PenaltyFacade.PricedPenalty(ZERO, penaltyPct, ZERO, ZERO, ZERO));

    return new OccurrenceCancellationTermsResponse(
        routeOccurrenceId,
        hoursOut,
        free,
        freeHours,
        free
            ? "Cancelling now is free — there is still time for your riders to find another trip."
            : "You are inside the "
                + freeHours
                + "-hour window, so a fee applies and half of it"
                + " goes to the riders you are standing up.",
        free ? ZERO : penaltyPct,
        priced.fareBase(),
        free ? ZERO : priced.feeAmount(),
        free ? ZERO : priced.victimShare(),
        free ? ZERO : priced.platformShare(),
        riders.stream().map(row -> row.getFirstName()).toList(),
        riders.size(),
        REASON_CODES);
  }

  @Override
  @Transactional
  public Map<String, Object> cancel(long routeOccurrenceId, OccurrenceCancellationRequest request) {
    long actorAppUserId = requireOwner(routeOccurrenceId);
    var context = context(routeOccurrenceId);
    if (!TripEditability.PUBLISHED.equalsIgnoreCase(context.getStatus())) {
      throw new GateConflictException(
          "OCCURRENCE_NOT_PUBLISHED",
          "This trip is no longer live.",
          "/driver/route-occurrences/" + routeOccurrenceId);
    }

    int freeHours = policy.integer(PolicyKey.DRIVER_CANCEL_FREE_HOURS);
    BigDecimal hoursOut = hoursUntil(context.getDepartsAt());
    boolean free = hoursOut.compareTo(BigDecimal.valueOf(freeHours)) >= 0;

    var riders = occurrences.findAffectedRiders(routeOccurrenceId);
    var tripId = bookings.findTripIdForOccurrence(routeOccurrenceId);

    // The penalty is priced from the bookings this is about to close, so it must be assessed while
    // they are still open. Cancelling first would price a trip that had already lost its riders.
    if (!free) {
      tripId.ifPresent(penalties::assessDriverLateCancellation);
    }

    occurrences.cancelOccurrence(routeOccurrenceId);
    var cancelled =
        bookings.cancelOpenBookingsForOccurrence(
            routeOccurrenceId,
            "Driver cancelled the trip: " + request.reasonCode(),
            actorAppUserId);

    // Holds are released before the money, and the money before the notification: a rider told
    // "cancelled, nothing charged" while an authorisation is still live is the one order that
    // produces a complaint nobody can answer.
    for (var booking : cancelled) {
      payments.voidForBooking(booking.bookingId(), "OCCURRENCE_CANCELLED");
    }

    cancellations.save(
        RouteOccurrenceCancellationEntity.of(
            routeOccurrenceId,
            actorAppUserId,
            request.reasonCode(),
            request.note(),
            hoursOut,
            free));

    for (var booking : cancelled) {
      notifications.notifyUser(
          booking.passengerAppUserId(),
          "TRIP_CANCELLED_BY_DRIVER",
          "Your driver cancelled",
          free
              ? "Your driver has cancelled this trip. You have not been charged — here are other"
                  + " trips on your route."
              : "Your driver has cancelled close to departure. You have not been charged, and a"
                  + " share of his fee is coming to you as ride credit.",
          Map.of(
              "routeOccurrenceId", String.valueOf(routeOccurrenceId),
              "bookingId", String.valueOf(booking.bookingId())));
    }

    events.publish(
        DomainEvent.of(
            "route_occurrence.cancelled",
            "route_occurrence",
            String.valueOf(routeOccurrenceId),
            """
            {"routeOccurrenceId":%d,"reasonCode":"%s","hoursBeforeDeparture":%s,\
"withinFreeWindow":%s,"affectedBookings":%d}"""
                .formatted(
                    routeOccurrenceId, request.reasonCode(), hoursOut, free, cancelled.size())));

    meters
        .counter("routeshare_occurrence_cancellations_total", "window", free ? "FREE" : "PENALISED")
        .increment();
    log.info(
        "occurrence {} cancelled {} h before departure ({}), {} bookings closed",
        routeOccurrenceId,
        hoursOut,
        free ? "free window" : "penalty window",
        cancelled.size());

    return Map.of(
        "routeOccurrenceId",
        routeOccurrenceId,
        "status",
        "CANCELLED",
        "withinFreeWindow",
        free,
        "hoursBeforeDeparture",
        hoursOut,
        "affectedBookings",
        cancelled.size(),
        "affectedRiders",
        riders.stream().map(row -> row.getFirstName()).toList());
  }

  // ── alternatives ─────────────────────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<AlternativeTripResponse> alternatives(long routeOccurrenceId) {
    return occurrences.findAlternatives(routeOccurrenceId).stream()
        .map(
            row ->
                new AlternativeTripResponse(
                    row.getRouteOccurrenceId(),
                    row.getDriverFirstName(),
                    row.getOriginLabel(),
                    row.getDestinationLabel(),
                    row.getDepartsAt(),
                    row.getSeatsAvailable() == null ? 0 : row.getSeatsAvailable(),
                    row.getRatePerKm(),
                    row.getMatchPercent()))
        .toList();
  }

  // ── helpers ──────────────────────────────────────────────────────────────────────────────────

  private RouteOccurrenceSeatRepository.OccurrenceContextRow context(long routeOccurrenceId) {
    return seats
        .findOccurrenceContext(routeOccurrenceId)
        .orElseThrow(() -> new NoSuchElementException("Route occurrence not found"));
  }

  /**
   * The driver whose trip this is — or an admin acting for them.
   *
   * <p>Ops needs this: a driver whose phone is dead still has riders waiting at a kerb, and
   * cancelling on his behalf is precisely the support case. The actor is recorded either way, so
   * the cancellation row always names whoever actually pressed it.
   */
  private long requireOwner(long routeOccurrenceId) {
    var user = current.requireCurrentUser();
    long appUserId = identityFacade.upsertFromToken(user).appUserId();
    if (occurrences.isOwnedByDriverAppUser(routeOccurrenceId, appUserId)) {
      return appUserId;
    }
    boolean admin =
        user.roles() != null
            && user.roles().stream()
                .anyMatch(
                    role ->
                        role.equals("ADMIN")
                            || role.equals("OPS_ADMIN")
                            || role.equals("SUPER_ADMIN"));
    if (!admin) {
      throw new AccessDeniedException("Trip does not belong to current driver");
    }
    return appUserId;
  }

  /** Hours to departure, floored at zero — a trip already gone is not "minus two hours out". */
  private BigDecimal hoursUntil(Instant departsAt) {
    long minutes = Duration.between(clock.instant(), departsAt).toMinutes();
    return BigDecimal.valueOf(Math.max(0, minutes))
        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
  }
}
