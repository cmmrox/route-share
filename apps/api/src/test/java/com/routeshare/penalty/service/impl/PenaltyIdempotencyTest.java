package com.routeshare.penalty.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.payment.facade.PaymentFacade;
import com.routeshare.penalty.domain.PenaltyKind;
import com.routeshare.penalty.entity.PenaltyAssessmentEntity;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Assessing a penalty twice must cost the passenger once.
 *
 * <p>Two sweeps can find the same released seat, and a driver can tap start twice. Either would be
 * a second charge on somebody's card, so the pre-read is only a courtesy: the guarantee is the
 * unique index, and the branch that handles losing that race is tested here explicitly.
 */
class PenaltyIdempotencyTest {
  private static final long BOOKING = 42L;
  private static final long TRIP = 7L;
  private static final long PASSENGER = 100L;
  private static final long DRIVER = 200L;
  private static final Instant NOW = Instant.parse("2026-08-02T09:00:00Z");

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

  private final PenaltyServiceImpl service =
      new PenaltyServiceImpl(
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
          Clock.fixed(NOW, ZoneOffset.UTC));

  @BeforeEach
  void setUp() {
    when(policy.decimal(PolicyKey.NO_SHOW_PENALTY_PCT)).thenReturn(new BigDecimal("25"));
    when(policy.decimal(PolicyKey.DRIVER_LATE_PENALTY_PCT)).thenReturn(new BigDecimal("20"));
    when(policy.decimal(PolicyKey.PENALTY_VICTIM_PCT)).thenReturn(new BigDecimal("50"));
    when(policy.pricingPolicyVersion()).thenReturn("v1");
    when(penalties.findPassengerAppUserId(BOOKING)).thenReturn(Optional.of(PASSENGER));
    when(penalties.findDriverAppUserIdForBooking(BOOKING)).thenReturn(Optional.of(DRIVER));
    when(penalties.findPassengerFare(BOOKING)).thenReturn(Optional.of(money("197")));
    when(penalties.findDriverNetForBooking(BOOKING)).thenReturn(Optional.of(money("251")));
    when(penalties.saveAndFlush(any())).thenAnswer(inv -> withId(inv.getArgument(0), 900L));
    when(penalties.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(beneficiaries.saveAll(any())).thenAnswer(inv -> List.copyOf(inv.getArgument(0)));
    when(beneficiaries.findByPenaltyId(anyLong())).thenReturn(List.of());
    when(disputes.findByPenaltyId(anyLong())).thenReturn(List.of());
    when(payments.collectPassengerPenalty(anyLong(), any()))
        .thenReturn(PaymentFacade.PenaltyCollection.NETTED);
  }

  @Test
  @DisplayName("A no-show is priced from the stored fare and split before anything is collected")
  void firstAssessmentPricesAndCollects() {
    var response = service.assessPassengerNoShow(BOOKING, TRIP);

    assertThat(response).isPresent();
    assertThat(response.get().feeAmount()).isEqualByComparingTo(money("49"));
    assertThat(response.get().victimShare()).isEqualByComparingTo(money("25"));
    assertThat(response.get().platformShare()).isEqualByComparingTo(money("24"));
    verify(payments).collectPassengerPenalty(eq(BOOKING), any());
  }

  @Test
  @DisplayName("A repeated trigger assesses nothing further and charges nothing further")
  void repeatedTriggerIsANoOp() {
    when(penalties.findByKindAndBookingId(PenaltyKind.PASSENGER_NO_SHOW.name(), BOOKING))
        .thenReturn(Optional.of(existingNoShow()));

    var response = service.assessPassengerNoShow(BOOKING, TRIP);

    assertThat(response).isPresent();
    assertThat(response.get().feeAmount()).isEqualByComparingTo(money("49"));
    verify(penalties, never()).saveAndFlush(any());
    verify(payments, never()).collectPassengerPenalty(anyLong(), any());
  }

  @Test
  @DisplayName("Losing the race to the unique index reads the winner's row rather than failing")
  void concurrentDuplicateFallsBackToTheWinningRow() {
    org.mockito.Mockito.doThrow(
            new DataIntegrityViolationException("penalty_assessment_booking_kind_uk"))
        .when(penalties)
        .saveAndFlush(any());
    when(penalties.findByKindAndBookingId(PenaltyKind.PASSENGER_NO_SHOW.name(), BOOKING))
        .thenReturn(Optional.empty(), Optional.of(existingNoShow()));

    var response = service.assessPassengerNoShow(BOOKING, TRIP);

    assertThat(response).isPresent();
    assertThat(response.get().feeAmount()).isEqualByComparingTo(money("49"));
    // The loser must not also charge her: exactly one transaction reached the gateway.
    verify(payments, never()).collectPassengerPenalty(anyLong(), any());
  }

  @Test
  @DisplayName("A cash passenger is never charged; the fee becomes a due")
  void cashPassengerFallsToDues() {
    when(payments.collectPassengerPenalty(anyLong(), any()))
        .thenReturn(PaymentFacade.PenaltyCollection.DUES);

    var response = service.assessPassengerNoShow(BOOKING, TRIP);

    assertThat(response).isPresent();
    assertThat(response.get().collection().method()).isEqualTo("DUES");
    verify(dues).recordDue(eq(PASSENGER), anyLong(), eq(money("49")), any(), eq(BOOKING));
  }

  @Test
  @DisplayName("A driver is deducted from earnings and never billed")
  void driverPenaltyIsADeductionNotACharge() {
    var response = service.assessDriverLate(BOOKING);

    assertThat(response).isPresent();
    assertThat(response.get().feeAmount()).isEqualByComparingTo(money("50"));
    assertThat(response.get().collection().method()).isEqualTo("EARNINGS_DEDUCTION");
    verify(payments).recordDriverPenaltyDeduction(BOOKING, money("50"));
    verify(payments, never()).collectPassengerPenalty(anyLong(), any());
  }

  @Test
  @DisplayName("Passenger and driver victims share one rewards ledger")
  void compensationUsesTheSharedRewardsBalance() {
    service.assessPassengerNoShow(BOOKING, TRIP);
    verify(rewards).credit(eq(DRIVER), eq(money("25")), any(), any());

    service.assessDriverLate(BOOKING);
    verify(rewards).credit(eq(PASSENGER), eq(money("25")), any(), any());
  }

  private PenaltyAssessmentEntity existingNoShow() {
    PenaltyAssessmentEntity entity =
        PenaltyAssessmentEntity.of(
            PenaltyKind.PASSENGER_NO_SHOW,
            BOOKING,
            TRIP,
            PASSENGER,
            money("197"),
            new BigDecimal("25"),
            new com.routeshare.penalty.domain.PenaltySplit(money("49"), money("25"), money("24")),
            "already assessed",
            NOW,
            "v1");
    return withId(entity, 900L);
  }

  private static PenaltyAssessmentEntity withId(PenaltyAssessmentEntity entity, long id) {
    ReflectionTestUtils.setField(entity, "id", id);
    return entity;
  }

  private static BigDecimal money(String value) {
    return new BigDecimal(value).setScale(2);
  }
}
