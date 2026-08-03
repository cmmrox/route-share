package com.routeshare.rewards.service.impl;

import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.event.DomainEvent;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.payment.facade.PaymentFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.rewards.domain.ReferralCodeGenerator;
import com.routeshare.rewards.domain.ReferralPolicy;
import com.routeshare.rewards.dto.request.AutoApplyRequest;
import com.routeshare.rewards.dto.request.ReferralClaimRequest;
import com.routeshare.rewards.dto.response.ReferralResponse;
import com.routeshare.rewards.dto.response.RewardsResponse;
import com.routeshare.rewards.dto.response.WithdrawalResponse;
import com.routeshare.rewards.repository.RewardsRepository;
import com.routeshare.rewards.service.RewardsService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class RewardsServiceImpl implements RewardsService {
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
  private static final Duration SIGNUP_WINDOW = Duration.ofHours(24);
  private static final BigDecimal RUNAWAY_SHARE = new BigDecimal("0.50");

  private final CurrentUserProvider current;
  private final IdentityFacade identity;
  private final RewardsRepository repository;
  private final ReferralCodeGenerator codes;
  private final PolicySettingService policy;
  private final PaymentFacade payments;
  private final DomainEventPublisher events;
  private final MeterRegistry meters;
  private final Clock clock;
  private final String referralLinkBaseUrl;

  public RewardsServiceImpl(
      CurrentUserProvider current,
      IdentityFacade identity,
      RewardsRepository repository,
      ReferralCodeGenerator codes,
      PolicySettingService policy,
      PaymentFacade payments,
      DomainEventPublisher events,
      MeterRegistry meters,
      Clock clock,
      @Value("${routeshare.rewards.referral-link-base-url:https://comigo.lk/j/}")
          String referralLinkBaseUrl) {
    this.current = current;
    this.identity = identity;
    this.repository = repository;
    this.codes = codes;
    this.policy = policy;
    this.payments = payments;
    this.events = events;
    this.meters = meters;
    this.clock = clock;
    this.referralLinkBaseUrl = referralLinkBaseUrl;
    Gauge.builder(
            "routeshare_rewards_balance_total",
            repository,
            source -> source.totalBalance().doubleValue())
        .description("Signed total of all shared rewards balances")
        .register(meters);
  }

  @Override
  @Transactional
  public ReferralResponse referral(String deviceId) {
    var app = identity.upsertFromToken(current.requireCurrentUser());
    ensureReferralCode(app.appUserId(), app.displayName());
    rememberDevice(app.appUserId(), deviceId);
    return referralFor(app.appUserId());
  }

  @Override
  @Transactional
  public ReferralResponse claim(ReferralClaimRequest request, String deviceId) {
    var app = identity.upsertFromToken(current.requireCurrentUser());
    ensureReferralCode(app.appUserId(), app.displayName());
    claimAtSignup(app.appUserId(), app.phone(), app.displayName(), request.code(), deviceId);
    return referralFor(app.appUserId());
  }

  @Override
  @Transactional(readOnly = true)
  public RewardsResponse rewards() {
    var app = identity.upsertFromToken(current.requireCurrentUser());
    return rewardsFor(app.appUserId());
  }

  @Override
  @Transactional
  public RewardsResponse setAutoApply(AutoApplyRequest request) {
    var app = identity.upsertFromToken(current.requireCurrentUser());
    if (repository.setAutoApply(app.appUserId(), request.enabled()) != 1) {
      throw new GateConflictException(
          "PASSENGER_PROFILE_REQUIRED",
          "Complete your passenger profile before changing ride-credit settings.",
          "/passenger/profile");
    }
    return rewardsFor(app.appUserId());
  }

  @Override
  @Transactional
  public WithdrawalResponse requestWithdrawal() {
    var app = identity.upsertFromToken(current.requireCurrentUser());
    ensureReferralCode(app.appUserId(), app.displayName());
    repository.lockAccount(app.appUserId()).orElseThrow();
    if (!repository.bankWithdrawalReady(app.appUserId())) {
      throw new GateConflictException(
          "BANK_ACCOUNT_REQUIRED",
          "Add and verify a driver bank account before requesting a withdrawal.",
          "/driver/payout-profile");
    }
    if (repository.hasOpenWithdrawal(app.appUserId())) {
      throw conflict(
          "WITHDRAWAL_ALREADY_QUEUED",
          "A rewards withdrawal is already waiting for the Friday batch.");
    }
    BigDecimal available = money(repository.balance(app.appUserId()));
    BigDecimal floor = money(policy.decimal(PolicyKey.REWARDS_BANK_MINIMUM));
    if (!ReferralPolicy.canWithdraw(available, floor)) {
      throw conflict(
          "REWARDS_BELOW_BANK_MINIMUM",
          "Bank withdrawals need at least LKR " + floor.toPlainString() + ".");
    }
    long withdrawalId;
    try {
      withdrawalId = repository.createWithdrawal(app.appUserId(), available);
    } catch (DataIntegrityViolationException duplicate) {
      throw conflict(
          "WITHDRAWAL_ALREADY_QUEUED",
          "A rewards withdrawal is already waiting for the Friday batch.");
    }
    repository.insertLedger(
        app.appUserId(),
        "WITHDRAWAL",
        available.negate(),
        "Bank withdrawal queued",
        "Next Friday payout batch",
        null,
        null,
        null,
        withdrawalId,
        clock.instant(),
        "withdrawal:" + withdrawalId);
    meters.counter("routeshare_withdrawals_total", "status", "QUEUED").increment();
    return repository.withdrawals(app.appUserId()).stream()
        .filter(row -> row.getId() == withdrawalId)
        .findFirst()
        .map(this::withdrawal)
        .orElseThrow();
  }

  @Override
  @Transactional(readOnly = true)
  public List<WithdrawalResponse> withdrawals() {
    var app = identity.upsertFromToken(current.requireCurrentUser());
    return repository.withdrawals(app.appUserId()).stream().map(this::withdrawal).toList();
  }

  @Override
  @Transactional
  public int expireReferralWindows() {
    return repository.expireWindows(clock.instant());
  }

  @Override
  @Transactional
  public void ensureReferralCode(long appUserId, String displayName) {
    if (repository.findCode(appUserId).isPresent()) {
      return;
    }
    for (int attempt = 0; attempt < 8; attempt++) {
      try {
        repository.ensureCode(appUserId, codes.generate(displayName));
      } catch (DataIntegrityViolationException collision) {
        // Entropy collision: retry a fresh suffix. The app-user conflict means another request won.
      }
      if (repository.findCode(appUserId).isPresent()) {
        return;
      }
    }
    throw new IllegalStateException("Could not allocate a unique referral code");
  }

  @Override
  @Transactional
  public void claimAtSignup(
      long appUserId, String phone, String displayName, String rawCode, String deviceId) {
    if (rawCode == null || rawCode.isBlank()) {
      return;
    }
    ensureReferralCode(appUserId, displayName);
    String code = rawCode.trim().toUpperCase(Locale.ROOT);
    var owner =
        repository
            .findCodeOwner(code)
            .orElseThrow(
                () -> conflict("REFERRAL_CODE_INVALID", "That referral code does not exist."));
    boolean sameDevice =
        deviceId != null
            && !deviceId.isBlank()
            && repository.deviceBelongsTo(owner.getAppUserId(), hashDevice(deviceId));
    if (ReferralPolicy.selfReferral(
        owner.getAppUserId(), appUserId, samePhone(owner.getPhone(), phone), sameDevice)) {
      meters.counter("routeshare_referral_rejections_total", "reason", "SELF").increment();
      throw conflict("REFERRAL_SELF_NOT_ALLOWED", "You cannot claim your own referral invitation.");
    }
    if (repository.alreadyAttributed(appUserId)) {
      throw conflict(
          "REFERRAL_ALREADY_ATTRIBUTED", "A referral has already been attached to this account.");
    }
    Instant now = clock.instant();
    Instant created = repository.accountCreatedAt(appUserId).orElse(now);
    if (now.isAfter(created.plus(SIGNUP_WINDOW)) || repository.hasBooking(appUserId)) {
      throw conflict(
          "REFERRAL_WINDOW_CLOSED",
          "Referral codes can only be claimed while setting up a new account.");
    }
    long edgeId;
    try {
      edgeId =
          repository.createEdge(
              owner.getAppUserId(),
              appUserId,
              code,
              now,
              now.atZone(ZoneOffset.UTC)
                  .plusMonths(policy.integer(PolicyKey.REFERRAL_WINDOW_MONTHS))
                  .toInstant(),
              policy.integer(PolicyKey.REFERRAL_MAX_TRIPS));
    } catch (DataIntegrityViolationException duplicate) {
      throw conflict(
          "REFERRAL_ALREADY_ATTRIBUTED", "A referral has already been attached to this account.");
    }
    rememberDevice(appUserId, deviceId);
    BigDecimal firstRide = money(policy.decimal(PolicyKey.REFEREE_FIRST_RIDE_DISCOUNT));
    repository.insertLedger(
        appUserId,
        "ADJUSTMENT",
        firstRide,
        "First ride referral discount",
        "Use it on your first ComiGo ride",
        null,
        null,
        edgeId,
        null,
        now,
        "referee-first-ride:" + edgeId);
    meters.counter("routeshare_referral_attributions_total").increment();
    events.publish(
        DomainEvent.of(
            "referral.attributed",
            "referral",
            String.valueOf(edgeId),
            "{\"edgeId\":" + edgeId + ",\"refereeAppUserId\":" + appUserId + "}"));
  }

  @Override
  @Transactional
  public BigDecimal applyRideCredit(
      long appUserId, long bookingId, BigDecimal fare, Boolean useRewardsCredit) {
    ensureReferralCode(appUserId, null);
    repository.lockAccount(appUserId).orElseThrow();
    boolean use =
        useRewardsCredit != null ? useRewardsCredit : repository.autoApply(appUserId).orElse(true);
    if (!use || fare == null || fare.signum() <= 0) {
      return ZERO;
    }
    BigDecimal amount = ReferralPolicy.rideCredit(repository.balance(appUserId), fare);
    if (amount.signum() <= 0) {
      return ZERO;
    }
    int inserted =
        repository.insertLedger(
            appUserId,
            "SPEND",
            amount.negate(),
            "Ride credit",
            "Applied to booking " + bookingId,
            bookingId,
            null,
            null,
            null,
            clock.instant(),
            "booking-credit:" + bookingId);
    return inserted == 1
        ? amount
        : repository.bookingSpend(appUserId, bookingId).orElse(ZERO).abs();
  }

  @Override
  @Transactional
  public BigDecimal releaseRideCredit(long appUserId, long bookingId) {
    ensureReferralCode(appUserId, null);
    repository.lockAccount(appUserId).orElseThrow();
    BigDecimal spent = repository.bookingSpend(appUserId, bookingId).orElse(ZERO).abs();
    if (spent.signum() <= 0) {
      return ZERO;
    }
    repository.insertLedger(
        appUserId,
        "ADJUSTMENT",
        spent,
        "Ride credit restored",
        "Booking " + bookingId + " ended before capture",
        bookingId,
        null,
        null,
        null,
        clock.instant(),
        "booking-credit-release:" + bookingId);
    return spent;
  }

  @Override
  @Transactional
  public void accrueCompletedTrip(long tripId) {
    Instant now = clock.instant();
    for (var participant : repository.tripParticipants(tripId)) {
      var maybeEdge = repository.lockEdgeForReferee(participant.getAppUserId());
      if (maybeEdge.isEmpty()) {
        continue;
      }
      var edge = maybeEdge.get();
      if (!ReferralPolicy.edgeCanAccrue(
          edge.getStatus(), edge.getExpiresAt(), edge.getTripsCounted(), edge.getMaxTrips(), now)) {
        continue;
      }
      BigDecimal rate =
          policy.decimal(
              "DRIVER".equals(participant.getRole())
                  ? PolicyKey.REFERRAL_DRIVER_PCT
                  : PolicyKey.REFERRAL_PAX_PCT);
      BigDecimal commission = money(participant.getCommission());
      var accrual = ReferralPolicy.accrue(participant.getBaseAmount(), rate, commission);
      BigDecimal requested = accrual.requested();
      BigDecimal credited = accrual.credited();
      String key = ReferralPolicy.accrualKey(edge.getEdgeId(), participant.getBookingId());
      int inserted =
          credited.signum() <= 0
              ? 0
              : repository.insertLedger(
                  edge.getReferrerId(),
                  "REFERRAL",
                  credited,
                  "Referral earning",
                  "DRIVER".equals(participant.getRole())
                      ? "They drove — 2% of what they kept"
                      : "They rode — 1% of the fare they paid",
                  participant.getBookingId(),
                  null,
                  edge.getEdgeId(),
                  null,
                  now,
                  key);
      if (inserted != 1) {
        continue;
      }
      payments.recordReferralPayout(participant.getBookingId(), key, credited, commission);
      repository.countTrip(edge.getEdgeId());
      meters
          .counter("routeshare_referral_accruals_total", "refereeRole", participant.getRole())
          .increment();
      meters.counter("routeshare_referral_cost_amount").increment(credited.doubleValue());
      BigDecimal shortfall = accrual.shortfall();
      if (shortfall.signum() > 0) {
        meters.counter("routeshare_referral_shortfall_amount").increment(shortfall.doubleValue());
        log.warn(
            "referral accrual capped edgeId={} bookingId={} requested={} commission={}",
            edge.getEdgeId(),
            participant.getBookingId(),
            requested,
            commission);
      }
      if (commission.signum() > 0
          && credited.divide(commission, 4, RoundingMode.HALF_UP).compareTo(RUNAWAY_SHARE) > 0) {
        meters.counter("routeshare_referral_cost_runaway_total").increment();
        log.error(
            "referral cost runaway edgeId={} bookingId={} cost={} commission={}",
            edge.getEdgeId(),
            participant.getBookingId(),
            credited,
            commission);
      }
      events.publish(
          DomainEvent.of(
              "reward.accrued",
              "referral",
              String.valueOf(edge.getEdgeId()),
              "{\"tripId\":"
                  + tripId
                  + ",\"bookingId\":"
                  + participant.getBookingId()
                  + ",\"role\":\""
                  + participant.getRole()
                  + "\",\"amount\":"
                  + credited
                  + ",\"shortfall\":"
                  + shortfall
                  + "}"));
    }
  }

  @Override
  @Transactional
  public String creditCompensation(
      long appUserId, BigDecimal amount, String reference, String description) {
    ensureReferralCode(appUserId, null);
    repository.insertLedger(
        appUserId,
        "COMPENSATION",
        money(amount),
        "Penalty compensation",
        description,
        null,
        penaltyId(reference),
        null,
        null,
        clock.instant(),
        reference);
    return reference;
  }

  @Override
  @Transactional(readOnly = true)
  public BigDecimal balance(long appUserId) {
    return money(repository.balance(appUserId));
  }

  private ReferralResponse referralFor(long appUserId) {
    String code = repository.findCode(appUserId).orElseThrow();
    var rows =
        repository.referrals(appUserId).stream()
            .map(
                row ->
                    new ReferralResponse.Row(
                        row.getWho(),
                        row.getRole(),
                        row.getJoinedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                        row.getTrips(),
                        row.getTripsLeft(),
                        money(row.getEarned()),
                        row.getStatus()))
            .toList();
    int active = (int) rows.stream().filter(row -> "ACTIVE".equals(row.status())).count();
    BigDecimal total =
        rows.stream().map(ReferralResponse.Row::earned).reduce(ZERO, BigDecimal::add);
    return new ReferralResponse(
        code,
        referralLinkBaseUrl + code,
        rows.size(),
        rows.size(),
        active,
        money(total),
        policy.decimal(PolicyKey.REFERRAL_PAX_PCT),
        policy.decimal(PolicyKey.REFERRAL_DRIVER_PCT),
        policy.integer(PolicyKey.REFERRAL_WINDOW_MONTHS),
        policy.integer(PolicyKey.REFERRAL_MAX_TRIPS),
        money(policy.decimal(PolicyKey.REFEREE_FIRST_RIDE_DISCOUNT)),
        rows);
  }

  private RewardsResponse rewardsFor(long appUserId) {
    BigDecimal balance = money(repository.balance(appUserId));
    BigDecimal floor = money(policy.decimal(PolicyKey.REWARDS_BANK_MINIMUM));
    return new RewardsResponse(
        balance,
        floor,
        balance.compareTo(floor) >= 0 ? balance : ZERO,
        balance.compareTo(floor) >= 0 ? ZERO : floor.subtract(balance).max(ZERO),
        repository.autoApply(appUserId).orElse(true),
        money(repository.referralEarned(appUserId)),
        repository.stillEarning(appUserId),
        repository.ledger(appUserId).stream()
            .map(
                row ->
                    new RewardsResponse.Row(
                        row.getId(),
                        row.getOccurredAt(),
                        "ADJUSTMENT".equals(row.getKind()) ? "COMPENSATION" : row.getKind(),
                        row.getLabel(),
                        row.getSublabel(),
                        money(row.getAmount())))
            .toList());
  }

  private WithdrawalResponse withdrawal(RewardsRepository.WithdrawalRow row) {
    return new WithdrawalResponse(
        row.getId(),
        money(row.getAmount()),
        row.getStatus(),
        row.getRequestedAt(),
        row.getBatchedAt(),
        row.getPaidAt(),
        row.getFailureReason());
  }

  private void rememberDevice(long appUserId, String deviceId) {
    if (deviceId != null && !deviceId.isBlank()) {
      repository.rememberDevice(appUserId, hashDevice(deviceId));
    }
  }

  private String hashDevice(String deviceId) {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(deviceId.trim().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  private boolean samePhone(String a, String b) {
    return a != null && !a.isBlank() && b != null && a.equals(b);
  }

  private Long penaltyId(String reference) {
    if (reference == null || !reference.matches("penalty:\\d+")) {
      return null;
    }
    return Long.valueOf(reference.substring("penalty:".length()));
  }

  private BigDecimal money(BigDecimal amount) {
    return (amount == null ? ZERO : amount).setScale(2, RoundingMode.HALF_UP);
  }

  private GateConflictException conflict(String code, String message) {
    return new GateConflictException(code, message, "/me/rewards");
  }
}
