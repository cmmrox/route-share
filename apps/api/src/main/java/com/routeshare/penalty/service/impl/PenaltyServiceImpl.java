package com.routeshare.penalty.service.impl;

import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.event.DomainEvent;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.payment.facade.PaymentFacade;
import com.routeshare.penalty.domain.PenaltyKind;
import com.routeshare.penalty.domain.PenaltyPolicy;
import com.routeshare.penalty.domain.PenaltyRole;
import com.routeshare.penalty.domain.PenaltySplit;
import com.routeshare.penalty.dto.request.PenaltyDisputeDecisionRequest;
import com.routeshare.penalty.dto.request.PenaltyDisputeRequest;
import com.routeshare.penalty.dto.response.PenaltyDisputeResponse;
import com.routeshare.penalty.dto.response.PenaltyResponse;
import com.routeshare.penalty.entity.PenaltyAssessmentEntity;
import com.routeshare.penalty.entity.PenaltyBeneficiaryEntity;
import com.routeshare.penalty.entity.PenaltyDisputeEntity;
import com.routeshare.penalty.repository.PenaltyAssessmentRepository;
import com.routeshare.penalty.repository.PenaltyBeneficiaryRepository;
import com.routeshare.penalty.repository.PenaltyDisputeRepository;
import com.routeshare.penalty.rewards.RewardsCreditPort;
import com.routeshare.penalty.service.DuesService;
import com.routeshare.penalty.service.PenaltyService;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The engine behind {@code POLICY.penaltyRecipient = "SPLIT"}.
 *
 * <p>Three properties hold for every assessment here, and each of them is the reason a specific
 * expensive failure cannot happen:
 *
 * <ol>
 *   <li><b>Assessment is idempotent through the database.</b> A partial unique index on {@code
 *       (kind, booking_id)} is the guard; the pre-read is only a courtesy. Two sweeps racing on the
 *       same released seat are two transactions, and no application check can arbitrate between
 *       them.
 *   <li><b>The halves are produced by subtraction.</b> Rounding both independently would create or
 *       destroy a rupee per penalty, and a database CHECK refuses the row if they ever disagree.
 *   <li><b>A driver is never billed.</b> His fee is a negative ledger line netted from what he
 *       earns next; D24 and D31 say so, and charging his card would be a different product.
 * </ol>
 *
 * <p>A reversal returns money to the payer but does <b>not</b> claw back the victim's half. She was
 * stood up whether or not his appeal succeeded, and taking ride credit back off someone who was
 * already let down is a second injury for a mistake that was never hers. The platform absorbs it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PenaltyServiceImpl implements PenaltyService {
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

  private final PenaltyAssessmentRepository penalties;
  private final PenaltyBeneficiaryRepository beneficiaries;
  private final PenaltyDisputeRepository disputes;
  private final PolicySettingService policy;
  private final PaymentFacade payments;
  private final DuesService dues;
  private final RewardsCreditPort rewards;
  private final NotificationFacade notifications;
  private final AdminAuditService audit;
  private final DomainEventPublisher events;
  private final MeterRegistry meters;
  private final Clock clock;

  // ── assessment ───────────────────────────────────────────────────────────────────────────────

  @Override
  @Transactional
  public Optional<PenaltyResponse> assessPassengerNoShow(long bookingId, Long tripId) {
    return penalties
        .findPassengerAppUserId(bookingId)
        .flatMap(
            payer ->
                assessAgainstBooking(
                    PenaltyKind.PASSENGER_NO_SHOW,
                    bookingId,
                    tripId,
                    payer,
                    penalties.findPassengerFare(bookingId).orElse(ZERO)));
  }

  @Override
  @Transactional
  public Optional<PenaltyResponse> assessPassengerCancelAfterStart(long bookingId, Long tripId) {
    return penalties
        .findPassengerAppUserId(bookingId)
        .flatMap(
            payer ->
                assessAgainstBooking(
                    PenaltyKind.PASSENGER_CANCEL_AFTER_START,
                    bookingId,
                    tripId,
                    payer,
                    penalties.findPassengerFare(bookingId).orElse(ZERO)));
  }

  @Override
  @Transactional
  public Optional<PenaltyResponse> assessDriverLate(long bookingId) {
    return penalties
        .findDriverAppUserIdForBooking(bookingId)
        .flatMap(
            payer ->
                assessAgainstBooking(
                    PenaltyKind.DRIVER_LATE,
                    bookingId,
                    null,
                    payer,
                    // His net for that seat, not her fare: the fee is a share of what he would have
                    // earned from her, which is the number D41 puts in front of him.
                    penalties.findDriverNetForBooking(bookingId).orElse(ZERO)));
  }

  @Override
  @Transactional
  public Optional<PenaltyResponse> assessDriverLateCancellation(long tripId) {
    Optional<Long> payer = penalties.findDriverAppUserIdForTrip(tripId);
    if (payer.isEmpty()) {
      return Optional.empty();
    }
    PenaltyKind kind = PenaltyKind.DRIVER_LATE_CANCELLATION;
    Optional<PenaltyAssessmentEntity> existing =
        penalties.findByKindAndTripIdAndBookingIdIsNull(kind.name(), tripId);
    if (existing.isPresent()) {
      return existing.map(this::render);
    }

    // Outside the free window there is no penalty at all — a driver who cancels a week ahead has
    // done nothing wrong, and the seats resell. The window is the whole rule, so it is checked here
    // rather than leaving every caller to remember it.
    Optional<Instant> departure = penalties.findDepartureForTrip(tripId);
    if (departure.isEmpty()
        || clock
            .instant()
            .isBefore(
                departure
                    .get()
                    .minus(Duration.ofHours(policy.integer(PolicyKey.DRIVER_CANCEL_FREE_HOURS))))) {
      return Optional.empty();
    }

    var affected = penalties.findConfirmedBookingsForTrip(tripId);
    List<Victim> victims =
        affected.stream()
            .filter(row -> row.getPassengerAppUserId() != null)
            .map(row -> new Victim(row.getPassengerAppUserId(), row.getBookingId()))
            .toList();

    return Optional.of(
        assess(
            kind,
            null,
            tripId,
            payer.get(),
            penalties.findExpectedNetForTrip(tripId),
            victims,
            // Anchored to the first affected booking so the deduction reaches his ledger through
            // the same join every other earnings row uses.
            victims.isEmpty() ? null : victims.get(0).bookingId()));
  }

  @Override
  @Transactional
  public Optional<PenaltyResponse> recordDriverMissedStart(long tripId) {
    Optional<Long> payer = penalties.findDriverAppUserIdForTrip(tripId);
    if (payer.isEmpty()) {
      return Optional.empty();
    }
    PenaltyKind kind = PenaltyKind.DRIVER_MISSED_START;
    Optional<PenaltyAssessmentEntity> existing =
        penalties.findByKindAndTripIdAndBookingIdIsNull(kind.name(), tripId);
    if (existing.isPresent()) {
      return existing.map(this::render);
    }
    // D32b is explicit: a missed start costs no fee. Losing the trip's earnings is the consequence,
    // and the reliability record is the one that matters. The row exists so a driver asking why his
    // month reads the way it does gets the same answer support does.
    return Optional.of(assess(kind, null, tripId, payer.get(), ZERO, List.of(), null));
  }

  private Optional<PenaltyResponse> assessAgainstBooking(
      PenaltyKind kind, long bookingId, Long tripId, long payerAppUserId, BigDecimal base) {
    Optional<PenaltyAssessmentEntity> existing =
        penalties.findByKindAndBookingId(kind.name(), bookingId);
    if (existing.isPresent()) {
      return existing.map(this::render);
    }
    Optional<Long> victim =
        kind.victimRole() == PenaltyRole.DRIVER
            ? penalties.findDriverAppUserIdForBooking(bookingId)
            : penalties.findPassengerAppUserId(bookingId);
    List<Victim> victims = victim.map(id -> List.of(new Victim(id, bookingId))).orElseGet(List::of);
    return Optional.of(assess(kind, bookingId, tripId, payerAppUserId, base, victims, bookingId));
  }

  /**
   * The one path every penalty takes: price it, split it, write it, collect it, credit it, say so.
   *
   * @param anchorBookingId the booking the ledger rows hang from; a trip-wide penalty has no
   *     booking of its own, so it borrows the first one it affected
   */
  private PenaltyResponse assess(
      PenaltyKind kind,
      Long bookingId,
      Long tripId,
      long payerAppUserId,
      BigDecimal base,
      List<Victim> victims,
      Long anchorBookingId) {
    Instant now = clock.instant();
    BigDecimal percent = kind.carriesFee() ? policy.decimal(kind.rateKey()) : ZERO;
    BigDecimal fee = PenaltyPolicy.fee(base, percent);
    PenaltySplit split = PenaltyPolicy.split(fee, policy.decimal(PolicyKey.PENALTY_VICTIM_PCT));

    PenaltyAssessmentEntity assessment;
    try {
      assessment =
          penalties.saveAndFlush(
              PenaltyAssessmentEntity.of(
                  kind,
                  bookingId,
                  tripId,
                  payerAppUserId,
                  base,
                  percent,
                  split,
                  explain(kind, percent, split),
                  now,
                  policy.pricingPolicyVersion()));
    } catch (DataIntegrityViolationException duplicate) {
      // The unique index arbitrated between two concurrent triggers. Whichever lost reads the
      // winner's row: the passenger is charged once, which is the entire point of the constraint.
      log.info("penalty {} already assessed for booking={} trip={}", kind, bookingId, tripId);
      return findExisting(kind, bookingId, tripId).map(this::render).orElseThrow(() -> duplicate);
    }

    List<PenaltyBeneficiaryEntity> credited = writeBeneficiaries(assessment, split, victims);
    String collection = collect(assessment, kind, anchorBookingId, split);
    creditVictims(assessment, kind, credited);

    assessment.settle(collection, now);
    penalties.save(assessment);

    announce(assessment, credited);
    meters
        .counter("routeshare_penalties_total", "kind", kind.name(), "collection", collection)
        .increment();
    log.info(
        "penalty {} assessed: base={} percent={} fee={} victim={} platform={} collection={}",
        kind,
        base,
        percent,
        split.fee(),
        split.victimShare(),
        split.platformShare(),
        collection);
    return render(assessment);
  }

  private List<PenaltyBeneficiaryEntity> writeBeneficiaries(
      PenaltyAssessmentEntity assessment, PenaltySplit split, List<Victim> victims) {
    if (victims.isEmpty() || split.victimShare().signum() <= 0) {
      return List.of();
    }
    // The victims arrive ordered by booking id, and the remainder of an odd half goes to the first.
    // Deterministic, and totalling exactly — a deferred constraint trigger fails the transaction if
    // it ever does not.
    List<BigDecimal> amounts = PenaltyPolicy.distribute(split.victimShare(), victims.size());
    List<PenaltyBeneficiaryEntity> rows = new ArrayList<>(victims.size());
    for (int i = 0; i < victims.size(); i++) {
      Victim victim = victims.get(i);
      rows.add(
          PenaltyBeneficiaryEntity.of(
              assessment.getId(), victim.appUserId(), victim.bookingId(), amounts.get(i)));
    }
    return beneficiaries.saveAll(rows);
  }

  private String collect(
      PenaltyAssessmentEntity assessment,
      PenaltyKind kind,
      Long anchorBookingId,
      PenaltySplit split) {
    if (split.fee().signum() <= 0 || anchorBookingId == null) {
      return PenaltyAssessmentEntity.COLLECTION_NONE;
    }
    if (kind.isDriverPaid()) {
      payments.recordDriverPenaltyDeduction(anchorBookingId, split.fee());
      return PenaltyAssessmentEntity.COLLECTION_EARNINGS_DEDUCTION;
    }
    PaymentFacade.PenaltyCollection taken =
        payments.collectPassengerPenalty(anchorBookingId, split.fee());
    if (taken == PaymentFacade.PenaltyCollection.DUES) {
      dues.recordDue(
          assessment.getPayerAppUserId(),
          assessment.getId(),
          split.fee(),
          label(kind),
          anchorBookingId);
      return PenaltyAssessmentEntity.COLLECTION_DUES;
    }
    return taken == PaymentFacade.PenaltyCollection.NETTED
        ? PenaltyAssessmentEntity.COLLECTION_NETTED
        : PenaltyAssessmentEntity.COLLECTION_CARD_CHARGE;
  }

  /**
   * Every victim's half lands in the shared rewards balance. Slice 11 deliberately makes that
   * balance role-neutral, so a person keeps the same credit when switching between passenger and
   * driver roles and every compensation remains visible through one ledger.
   */
  private void creditVictims(
      PenaltyAssessmentEntity assessment, PenaltyKind kind, List<PenaltyBeneficiaryEntity> rows) {
    Instant now = clock.instant();
    for (PenaltyBeneficiaryEntity row : rows) {
      if (row.getAmount().signum() <= 0) {
        continue;
      }
      String reference = "penalty:" + assessment.getId();
      rewards.credit(
          row.getBeneficiaryAppUserId(),
          row.getAmount(),
          reference,
          "Your "
              + victimPercent()
              + "% share of a penalty on "
              + label(kind).toLowerCase(java.util.Locale.ROOT));
      row.credited(now, reference);
    }
    beneficiaries.saveAll(rows);
  }

  private void announce(
      PenaltyAssessmentEntity assessment, List<PenaltyBeneficiaryEntity> credited) {
    PenaltyKind kind = assessment.kindEnum();
    if (assessment.getFeeAmount().signum() > 0) {
      notifications.notifyUser(
          assessment.getPayerAppUserId(),
          "PENALTY_ASSESSED",
          label(kind),
          assessment.getExplanation(),
          Map.of(
              "penaltyId", String.valueOf(assessment.getId()),
              "amount", assessment.getFeeAmount().toPlainString()));
    }
    for (PenaltyBeneficiaryEntity row : credited) {
      notifications.notifyUser(
          row.getBeneficiaryAppUserId(),
          "PENALTY_COMPENSATION",
          "Your " + victimPercent() + "% share",
          label(kind) + " — " + row.getAmount().toPlainString() + " is yours.",
          Map.of("penaltyId", String.valueOf(assessment.getId())));
    }
    // The audit trail: the rule, the policy version and every computed input, so a support agent
    // can explain a fee without reconstructing it. A penalty that cannot be explained is a refund.
    events.publish(
        DomainEvent.of(
            "penalty.assessed",
            "penalty",
            String.valueOf(assessment.getId()),
            """
            {"penaltyId":%d,"kind":"%s","bookingId":%s,"tripId":%s,"payerAppUserId":%d,\
"fareBase":%s,"percent":%s,"feeAmount":%s,"victimShare":%s,"platformShare":%s,\
"collection":"%s","policyVersion":"%s","beneficiaries":%d}"""
                .formatted(
                    assessment.getId(),
                    assessment.getKind(),
                    assessment.getBookingId(),
                    assessment.getTripId(),
                    assessment.getPayerAppUserId(),
                    assessment.getFareBase(),
                    assessment.getPercent(),
                    assessment.getFeeAmount(),
                    assessment.getVictimShare(),
                    assessment.getPlatformShare(),
                    assessment.getCollectionMethod(),
                    assessment.getPolicyVersion(),
                    credited.size())));
  }

  // ── reads ────────────────────────────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<PenaltyResponse> listForUser(long appUserId) {
    Map<Long, PenaltyAssessmentEntity> byId = new LinkedHashMap<>();
    penalties
        .findByPayerAppUserIdOrderByAssessedAtDesc(appUserId)
        .forEach(p -> byId.put(p.getId(), p));
    beneficiaries
        .findByBeneficiaryAppUserIdOrderByIdDesc(appUserId)
        .forEach(
            row -> penalties.findById(row.getPenaltyId()).ifPresent(p -> byId.put(p.getId(), p)));
    return byId.values().stream()
        .sorted((a, b) -> b.getAssessedAt().compareTo(a.getAssessedAt()))
        .map(p -> render(p, appUserId))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<PenaltyResponse> adminSearch(String kind, String status) {
    return penalties.search(blankToNull(kind), blankToNull(status)).stream()
        .map(this::render)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<PenaltyDisputeResponse> adminDisputes(String status) {
    return disputes.search(blankToNull(status)).stream().map(this::renderDispute).toList();
  }

  // ── disputes ─────────────────────────────────────────────────────────────────────────────────

  @Override
  @Transactional
  public PenaltyResponse dispute(long penaltyId, long appUserId, PenaltyDisputeRequest request) {
    PenaltyAssessmentEntity assessment = require(penaltyId);
    // Only the person charged may argue. A beneficiary disputing somebody else's fee would be
    // arguing to give money back that is not theirs to return.
    if (assessment.getPayerAppUserId() != appUserId) {
      throw new AccessDeniedException("Penalty does not belong to current user");
    }
    Instant now = clock.instant();
    Instant closes = assessment.getAssessedAt().plus(disputeWindow());
    if (now.isAfter(closes)) {
      throw new GateConflictException(
          "DISPUTE_WINDOW_CLOSED",
          "This fee can no longer be disputed. Contact support if you still need help.",
          "/support");
    }
    if (disputes.findOpenForPenalty(penaltyId).isPresent()) {
      throw new GateConflictException(
          "PENALTY_ALREADY_DISPUTED",
          "You have already disputed this fee and we are looking at it.",
          "/penalties/" + penaltyId);
    }
    try {
      disputes.saveAndFlush(
          PenaltyDisputeEntity.opened(penaltyId, appUserId, request.reason(), request.note(), now));
    } catch (DataIntegrityViolationException duplicate) {
      throw new GateConflictException(
          "PENALTY_ALREADY_DISPUTED",
          "You have already disputed this fee and we are looking at it.",
          "/penalties/" + penaltyId);
    }
    meters.counter("routeshare_penalty_disputes_total", "status", "OPEN").increment();
    return render(assessment, appUserId);
  }

  @Override
  @Transactional
  public PenaltyDisputeResponse decide(
      long disputeId, long adminAppUserId, PenaltyDisputeDecisionRequest request) {
    PenaltyDisputeEntity dispute =
        disputes
            .findById(disputeId)
            .orElseThrow(() -> new NoSuchElementException("Dispute not found"));
    if (!dispute.isOpen()) {
      throw new GateConflictException(
          "PENALTY_ALREADY_DISPUTED",
          "This dispute has already been decided.",
          "/admin/penalty-disputes");
    }
    PenaltyAssessmentEntity assessment = require(dispute.getPenaltyId());
    Instant now = clock.instant();
    boolean reversed = PenaltyDisputeEntity.STATUS_REVERSED.equals(request.decision());

    BigDecimal reversal = ZERO;
    if (reversed) {
      reversal =
          request.reverseAmount() == null
              ? assessment.getFeeAmount()
              : request.reverseAmount().min(assessment.getFeeAmount());
      reverse(assessment, reversal);
    }
    dispute.decide(
        reversed ? PenaltyDisputeEntity.STATUS_REVERSED : PenaltyDisputeEntity.STATUS_UPHELD,
        adminAppUserId,
        request.note(),
        reversed ? reversal : null,
        now);
    disputes.save(dispute);

    notifications.notifyUser(
        assessment.getPayerAppUserId(),
        "PENALTY_DISPUTE_DECIDED",
        reversed ? "Your fee has been reversed" : "We have reviewed your fee",
        reversed
            ? reversal.toPlainString() + " has been returned to you."
            : "After reviewing it, the fee stands.",
        Map.of("penaltyId", String.valueOf(assessment.getId())));

    audit.record(
        "PENALTY_DISPUTE_DECIDED",
        "penalty",
        String.valueOf(assessment.getId()),
        """
        {"disputeId":%d,"decision":"%s","reversedAmount":%s,"feeAmount":%s,"kind":"%s",\
"policyVersion":"%s"}"""
            .formatted(
                disputeId,
                dispute.getStatus(),
                reversed ? reversal : null,
                assessment.getFeeAmount(),
                assessment.getKind(),
                assessment.getPolicyVersion()));
    meters.counter("routeshare_penalty_disputes_total", "status", dispute.getStatus()).increment();
    return renderDispute(dispute);
  }

  private void reverse(PenaltyAssessmentEntity assessment, BigDecimal amount) {
    Long anchor = assessment.getBookingId();
    if (anchor == null) {
      anchor =
          beneficiaries.findByPenaltyId(assessment.getId()).stream()
              .map(PenaltyBeneficiaryEntity::getBookingId)
              .filter(java.util.Objects::nonNull)
              .findFirst()
              .orElse(null);
    }
    if (PenaltyAssessmentEntity.COLLECTION_DUES.equals(assessment.getCollectionMethod())) {
      dues.waiveDueForPenalty(assessment.getId());
    } else if (anchor != null && amount.signum() > 0) {
      if (assessment.kindEnum().isDriverPaid()) {
        payments.reverseDriverPenaltyDeduction(anchor, amount);
      } else {
        payments.reversePassengerPenalty(anchor, amount);
      }
    }
    assessment.reverse(clock.instant());
    penalties.save(assessment);
    events.publish(
        DomainEvent.of(
            "penalty.reversed",
            "penalty",
            String.valueOf(assessment.getId()),
            """
            {"penaltyId":%d,"kind":"%s","reversedAmount":%s,"feeAmount":%s,"policyVersion":"%s"}"""
                .formatted(
                    assessment.getId(),
                    assessment.getKind(),
                    amount,
                    assessment.getFeeAmount(),
                    assessment.getPolicyVersion())));
  }

  // ── rendering ────────────────────────────────────────────────────────────────────────────────

  private PenaltyResponse render(PenaltyAssessmentEntity assessment) {
    return render(assessment, assessment.getPayerAppUserId());
  }

  private PenaltyResponse render(PenaltyAssessmentEntity assessment, long viewerAppUserId) {
    List<PenaltyBeneficiaryEntity> rows = beneficiaries.findByPenaltyId(assessment.getId());
    BigDecimal amountForViewer =
        assessment.getPayerAppUserId() == viewerAppUserId
            ? assessment.getFeeAmount().negate()
            : rows.stream()
                .filter(row -> row.getBeneficiaryAppUserId() == viewerAppUserId)
                .map(PenaltyBeneficiaryEntity::getAmount)
                .reduce(ZERO, BigDecimal::add);

    Optional<PenaltyDisputeEntity> latestDispute =
        disputes.findByPenaltyId(assessment.getId()).stream().findFirst();

    return new PenaltyResponse(
        assessment.getId(),
        assessment.getKind(),
        assessment.getBookingId(),
        assessment.getTripId(),
        assessment.getFareBase(),
        assessment.getPercent(),
        assessment.getFeeAmount(),
        assessment.getVictimShare(),
        assessment.getPlatformShare(),
        assessment.getPayerRole(),
        assessment.getVictimRole(),
        amountForViewer,
        new PenaltyResponse.Collection(
            assessment.getCollectionMethod(), assessment.getStatus(), assessment.getSettledAt()),
        latestDispute.map(PenaltyDisputeEntity::getStatus).orElse("NONE"),
        isDisputable(assessment, latestDispute),
        assessment.getAssessedAt(),
        assessment.getExplanation(),
        rows.stream()
            .map(
                row ->
                    new PenaltyResponse.Beneficiary(
                        penalties.findFirstName(row.getBeneficiaryAppUserId()).orElse("A rider"),
                        row.getAmount()))
            .toList());
  }

  private boolean isDisputable(
      PenaltyAssessmentEntity assessment, Optional<PenaltyDisputeEntity> latestDispute) {
    if (assessment.getFeeAmount().signum() <= 0 || assessment.isReversed()) {
      return false;
    }
    if (latestDispute.isPresent()) {
      return false;
    }
    return !clock.instant().isAfter(assessment.getAssessedAt().plus(disputeWindow()));
  }

  private PenaltyDisputeResponse renderDispute(PenaltyDisputeEntity dispute) {
    PenaltyAssessmentEntity assessment = require(dispute.getPenaltyId());
    return new PenaltyDisputeResponse(
        dispute.getId(),
        dispute.getPenaltyId(),
        assessment.getKind(),
        assessment.getFeeAmount(),
        dispute.getRaisedByAppUserId(),
        dispute.getReason(),
        dispute.getNote(),
        dispute.getStatus(),
        dispute.getRaisedAt(),
        dispute.getDecidedAt(),
        dispute.getDecisionNote(),
        dispute.getReversedAmount());
  }

  // ── helpers ──────────────────────────────────────────────────────────────────────────────────

  private Optional<PenaltyAssessmentEntity> findExisting(
      PenaltyKind kind, Long bookingId, Long tripId) {
    if (bookingId != null) {
      return penalties.findByKindAndBookingId(kind.name(), bookingId);
    }
    return tripId == null
        ? Optional.empty()
        : penalties.findByKindAndTripIdAndBookingIdIsNull(kind.name(), tripId);
  }

  private PenaltyAssessmentEntity require(long penaltyId) {
    return penalties
        .findById(penaltyId)
        .orElseThrow(() -> new NoSuchElementException("Penalty not found"));
  }

  private Duration disputeWindow() {
    return Duration.ofHours(policy.integer(PolicyKey.PENALTY_DISPUTE_WINDOW_HOURS));
  }

  private String victimPercent() {
    return policy.decimal(PolicyKey.PENALTY_VICTIM_PCT).stripTrailingZeros().toPlainString();
  }

  private static String label(PenaltyKind kind) {
    return switch (kind) {
      case PASSENGER_CANCEL_AFTER_START -> "Cancelled after the trip started";
      case PASSENGER_NO_SHOW -> "No-show fee";
      case DRIVER_LATE -> "Late to pickup";
      case DRIVER_LATE_CANCELLATION -> "Late-cancellation penalty";
      case DRIVER_MISSED_START -> "Missed start";
    };
  }

  private String explain(PenaltyKind kind, BigDecimal percent, PenaltySplit split) {
    if (!kind.carriesFee() || split.fee().signum() <= 0) {
      return "No fee applies. The trip earned nothing and the miss is on your record.";
    }
    String pct = percent.stripTrailingZeros().toPlainString();
    return "%s: %s%% of %s is %s. %s goes to the person it let down and %s to ComiGo."
        .formatted(
            label(kind),
            pct,
            kind.isDriverPaid() ? "your expected earnings" : "the fare",
            split.fee().toPlainString(),
            split.victimShare().toPlainString(),
            split.platformShare().toPlainString());
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  /** Someone a penalty is owed to, and the booking that entitles them to it. */
  private record Victim(long appUserId, Long bookingId) {}
}
