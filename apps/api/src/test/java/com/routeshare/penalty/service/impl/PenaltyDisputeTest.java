package com.routeshare.penalty.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.payment.facade.PaymentFacade;
import com.routeshare.penalty.domain.PenaltyKind;
import com.routeshare.penalty.domain.PenaltySplit;
import com.routeshare.penalty.dto.request.PenaltyDisputeDecisionRequest;
import com.routeshare.penalty.dto.request.PenaltyDisputeRequest;
import com.routeshare.penalty.entity.PenaltyAssessmentEntity;
import com.routeshare.penalty.entity.PenaltyDisputeEntity;
import com.routeshare.penalty.repository.PenaltyAssessmentRepository;
import com.routeshare.penalty.repository.PenaltyBeneficiaryRepository;
import com.routeshare.penalty.repository.PenaltyDisputeRepository;
import com.routeshare.penalty.rewards.RewardsCreditPort;
import com.routeshare.penalty.service.DuesService;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Disputes, and the two boundaries that decide whether one is heard at all.
 *
 * <p>The window is a real cut-off — 48 hours after assessment — because a fee argued about three
 * months later cannot be checked against a driver's memory or a location trail that has since been
 * pruned. And only the person charged may argue: a beneficiary disputing somebody else's fee would
 * be arguing to give back money that was never theirs.
 */
class PenaltyDisputeTest {
  private static final long PENALTY = 900L;
  private static final long BOOKING = 42L;
  private static final long PAYER = 100L;
  private static final long SOMEBODY_ELSE = 101L;
  private static final long ADMIN = 999L;
  private static final Instant ASSESSED = Instant.parse("2026-08-01T09:00:00Z");

  private final PenaltyAssessmentRepository penalties = mock(PenaltyAssessmentRepository.class);
  private final PenaltyBeneficiaryRepository beneficiaries =
      mock(PenaltyBeneficiaryRepository.class);
  private final PenaltyDisputeRepository disputes = mock(PenaltyDisputeRepository.class);
  private final PolicySettingService policy = mock(PolicySettingService.class);
  private final PaymentFacade payments = mock(PaymentFacade.class);
  private final DuesService dues = mock(DuesService.class);
  private final RewardsCreditPort rewards = mock(RewardsCreditPort.class);
  private final NotificationFacade notifications = mock(NotificationFacade.class);
  private final AdminAuditService audit = mock(AdminAuditService.class);
  private final DomainEventPublisher events = mock(DomainEventPublisher.class);

  private PenaltyServiceImpl serviceAt(Instant now) {
    when(policy.integer(PolicyKey.PENALTY_DISPUTE_WINDOW_HOURS)).thenReturn(48);
    when(policy.decimal(PolicyKey.PENALTY_VICTIM_PCT)).thenReturn(new BigDecimal("50"));
    when(beneficiaries.findByPenaltyId(anyLong())).thenReturn(List.of());
    when(disputes.findByPenaltyId(anyLong())).thenReturn(List.of());
    when(disputes.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    when(disputes.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(penalties.save(any())).thenAnswer(inv -> inv.getArgument(0));
    return new PenaltyServiceImpl(
        penalties,
        beneficiaries,
        disputes,
        policy,
        payments,
        dues,
        rewards,
        notifications,
        audit,
        events,
        new SimpleMeterRegistry(),
        Clock.fixed(now, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("A dispute raised at 47 hours is accepted")
  void withinTheWindow() {
    var service = serviceAt(ASSESSED.plus(Duration.ofHours(47)));
    when(penalties.findById(PENALTY))
        .thenReturn(Optional.of(assessment(PenaltyKind.PASSENGER_NO_SHOW)));
    when(disputes.findOpenForPenalty(PENALTY)).thenReturn(Optional.empty());

    var response = service.dispute(PENALTY, PAYER, new PenaltyDisputeRequest("I was there", null));

    assertThat(response.id()).isEqualTo(PENALTY);
    verify(disputes).saveAndFlush(any());
  }

  @Test
  @DisplayName("A dispute raised at 49 hours is refused with DISPUTE_WINDOW_CLOSED")
  void outsideTheWindow() {
    var service = serviceAt(ASSESSED.plus(Duration.ofHours(49)));
    when(penalties.findById(PENALTY))
        .thenReturn(Optional.of(assessment(PenaltyKind.PASSENGER_NO_SHOW)));

    assertThatThrownBy(
            () -> service.dispute(PENALTY, PAYER, new PenaltyDisputeRequest("I was there", null)))
        .isInstanceOf(GateConflictException.class)
        .hasMessageContaining("no longer be disputed");
    verify(disputes, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("Disputing the same fee twice is the same argument, not a second case")
  void alreadyDisputed() {
    var service = serviceAt(ASSESSED.plus(Duration.ofHours(1)));
    when(penalties.findById(PENALTY))
        .thenReturn(Optional.of(assessment(PenaltyKind.PASSENGER_NO_SHOW)));
    when(disputes.findOpenForPenalty(PENALTY))
        .thenReturn(
            Optional.of(PenaltyDisputeEntity.opened(PENALTY, PAYER, "again", null, ASSESSED)));

    assertThatThrownBy(
            () -> service.dispute(PENALTY, PAYER, new PenaltyDisputeRequest("again", null)))
        .isInstanceOf(GateConflictException.class);
  }

  @Test
  @DisplayName("Nobody may dispute a penalty that is not theirs")
  void otherPeoplesPenaltiesAreNotDisputable() {
    var service = serviceAt(ASSESSED.plus(Duration.ofHours(1)));
    when(penalties.findById(PENALTY))
        .thenReturn(Optional.of(assessment(PenaltyKind.PASSENGER_NO_SHOW)));

    assertThatThrownBy(
            () ->
                service.dispute(
                    PENALTY, SOMEBODY_ELSE, new PenaltyDisputeRequest("not mine", null)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("An upheld dispute moves no money")
  void upheldLeavesTheFeeStanding() {
    var service = serviceAt(ASSESSED.plus(Duration.ofHours(2)));
    var assessment = assessment(PenaltyKind.PASSENGER_NO_SHOW);
    when(penalties.findById(PENALTY)).thenReturn(Optional.of(assessment));
    when(disputes.findById(1L)).thenReturn(Optional.of(openDispute()));

    var decided =
        service.decide(1L, ADMIN, new PenaltyDisputeDecisionRequest("UPHELD", "Checked", null));

    assertThat(decided.status()).isEqualTo("UPHELD");
    assertThat(assessment.isReversed()).isFalse();
    verify(payments, never()).reversePassengerPenalty(anyLong(), any());
  }

  @Test
  @DisplayName("A reversed passenger penalty goes back to her card, defaulting to the whole fee")
  void reversedPassengerPenaltyIsRefunded() {
    var service = serviceAt(ASSESSED.plus(Duration.ofHours(2)));
    var assessment = assessment(PenaltyKind.PASSENGER_NO_SHOW);
    assessment.settle(PenaltyAssessmentEntity.COLLECTION_NETTED, ASSESSED);
    when(penalties.findById(PENALTY)).thenReturn(Optional.of(assessment));
    when(disputes.findById(1L)).thenReturn(Optional.of(openDispute()));

    var decided =
        service.decide(1L, ADMIN, new PenaltyDisputeDecisionRequest("REVERSED", "Our error", null));

    assertThat(decided.status()).isEqualTo("REVERSED");
    assertThat(decided.reversedAmount()).isEqualByComparingTo(money("49"));
    assertThat(assessment.isReversed()).isTrue();
    verify(payments).reversePassengerPenalty(BOOKING, money("49"));
    verify(audit).record(any(), any(), any(), any());
  }

  @Test
  @DisplayName("A reversed driver penalty gives the deduction back rather than refunding a card")
  void reversedDriverPenaltyGivesBackTheDeduction() {
    var service = serviceAt(ASSESSED.plus(Duration.ofHours(2)));
    var assessment = assessment(PenaltyKind.DRIVER_LATE);
    assessment.settle(PenaltyAssessmentEntity.COLLECTION_EARNINGS_DEDUCTION, ASSESSED);
    when(penalties.findById(PENALTY)).thenReturn(Optional.of(assessment));
    when(disputes.findById(1L)).thenReturn(Optional.of(openDispute()));

    service.decide(1L, ADMIN, new PenaltyDisputeDecisionRequest("REVERSED", "He was there", null));

    verify(payments).reverseDriverPenaltyDeduction(BOOKING, money("49"));
    verify(payments, never()).reversePassengerPenalty(anyLong(), any());
  }

  @Test
  @DisplayName("A reversed fee that fell to dues is waived, never refunded — nothing was taken")
  void reversedDuesAreWaived() {
    var service = serviceAt(ASSESSED.plus(Duration.ofHours(2)));
    var assessment = assessment(PenaltyKind.PASSENGER_NO_SHOW);
    assessment.settle(PenaltyAssessmentEntity.COLLECTION_DUES, ASSESSED);
    when(penalties.findById(PENALTY)).thenReturn(Optional.of(assessment));
    when(disputes.findById(1L)).thenReturn(Optional.of(openDispute()));

    service.decide(1L, ADMIN, new PenaltyDisputeDecisionRequest("REVERSED", "Waived", null));

    verify(dues).waiveDueForPenalty(PENALTY);
    verify(payments, never()).reversePassengerPenalty(anyLong(), any());
  }

  @Test
  @DisplayName("A partial reversal cannot return more than was charged")
  void partialReversalIsCappedAtTheFee() {
    var service = serviceAt(ASSESSED.plus(Duration.ofHours(2)));
    var assessment = assessment(PenaltyKind.PASSENGER_NO_SHOW);
    assessment.settle(PenaltyAssessmentEntity.COLLECTION_NETTED, ASSESSED);
    when(penalties.findById(PENALTY)).thenReturn(Optional.of(assessment));
    when(disputes.findById(1L)).thenReturn(Optional.of(openDispute()));

    var decided =
        service.decide(
            1L, ADMIN, new PenaltyDisputeDecisionRequest("REVERSED", "Half", money("500")));

    assertThat(decided.reversedAmount()).isEqualByComparingTo(money("49"));
  }

  @Test
  @DisplayName("A decided dispute cannot be decided again")
  void decidingTwiceIsRefused() {
    var service = serviceAt(ASSESSED.plus(Duration.ofHours(2)));
    var dispute = openDispute();
    dispute.decide(PenaltyDisputeEntity.STATUS_UPHELD, ADMIN, "done", null, ASSESSED);
    when(disputes.findById(1L)).thenReturn(Optional.of(dispute));

    assertThatThrownBy(
            () -> service.decide(1L, ADMIN, new PenaltyDisputeDecisionRequest("UPHELD", "", null)))
        .isInstanceOf(GateConflictException.class);
  }

  private PenaltyDisputeEntity openDispute() {
    var dispute =
        PenaltyDisputeEntity.opened(PENALTY, PAYER, "I was there", null, ASSESSED.plusSeconds(60));
    ReflectionTestUtils.setField(dispute, "id", 1L);
    return dispute;
  }

  private PenaltyAssessmentEntity assessment(PenaltyKind kind) {
    var entity =
        PenaltyAssessmentEntity.of(
            kind,
            BOOKING,
            null,
            PAYER,
            money("197"),
            new BigDecimal("25"),
            new PenaltySplit(money("49"), money("25"), money("24")),
            "explained",
            ASSESSED,
            "v1");
    ReflectionTestUtils.setField(entity, "id", PENALTY);
    return entity;
  }

  private static BigDecimal money(String value) {
    return new BigDecimal(value).setScale(2);
  }
}
