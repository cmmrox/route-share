package com.routeshare.reliability.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.reliability.domain.ReliabilityEventType;
import com.routeshare.reliability.domain.ReliabilityRole;
import com.routeshare.reliability.entity.MonthlyCounterEntity;
import com.routeshare.reliability.service.ReliabilityService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * 05-16 and 05-17. The third drop is not refused — she is getting out of the car either way — the
 * fare simply stands, and the response has to say so as data rather than as an error.
 */
class EarlyDropAllowanceServiceImplTest {
  private static final LocalDate AUGUST = LocalDate.of(2026, 8, 1);
  private static final long PASSENGER = 12L;

  private final ReliabilityService reliability = mock(ReliabilityService.class);
  private final PolicySettingService policy = mock(PolicySettingService.class);
  private final EarlyDropAllowanceServiceImpl service =
      new EarlyDropAllowanceServiceImpl(reliability, policy);

  private void usedThisMonth(int used) {
    when(reliability.currentPeriod()).thenReturn(AUGUST);
    when(policy.integer(PolicyKey.EARLY_DROP_ADJUSTED_PER_MONTH)).thenReturn(2);
    var counter = MonthlyCounterEntity.opened(PASSENGER, ReliabilityRole.PASSENGER, AUGUST);
    counter.setEarlyDropsAdjusted(used);
    when(reliability.counter(PASSENGER, ReliabilityRole.PASSENGER, AUGUST)).thenReturn(counter);
  }

  /** 05-16: the first drop of the month is adjusted. */
  @Test
  void theFirstDropIsAdjusted() {
    usedThisMonth(0);

    assertThat(service.consume(PASSENGER, 100L, null)).isTrue();
    verify(reliability)
        .record(
            anyLong(),
            any(),
            org.mockito.ArgumentMatchers.eq(ReliabilityEventType.EARLY_DROP_ADJUSTED),
            any(),
            any(),
            any());
  }

  /** 05-16: so is the second. */
  @Test
  void theSecondDropIsAdjusted() {
    usedThisMonth(1);

    assertThat(service.consume(PASSENGER, 100L, null)).isTrue();
  }

  /**
   * 05-17: the third is not. Nothing is recorded either — counting an unadjusted drop would make
   * next month's allowance run out faster than the rule she was shown.
   */
  @Test
  void theThirdDropIsNotAdjustedAndIsNotRecorded() {
    usedThisMonth(2);

    assertThat(service.consume(PASSENGER, 100L, null)).isFalse();
    verify(reliability, never()).record(anyLong(), any(), any(), any(), any(), any());
  }

  /** P16 has to be able to say this before she taps, not after she is charged. */
  @Test
  void theAllowanceIsReadableBeforeSheCommits() {
    usedThisMonth(1);

    var allowance = service.allowance(PASSENGER);

    assertThat(allowance.month()).isEqualTo(AUGUST);
    assertThat(allowance.used()).isEqualTo(1);
    assertThat(allowance.allowance()).isEqualTo(2);
    assertThat(allowance.remaining()).isEqualTo(1);
    assertThat(allowance.nextDropWillBeAdjusted()).isTrue();
  }

  /** P16b: exhausted, and the screen can say so. */
  @Test
  void anExhaustedAllowanceReportsZeroRemaining() {
    usedThisMonth(2);

    var allowance = service.allowance(PASSENGER);

    assertThat(allowance.remaining()).isZero();
    assertThat(allowance.nextDropWillBeAdjusted()).isFalse();
  }

  /**
   * A correction that pushed the count above the allowance must not report a negative remainder.
   */
  @Test
  void anOverCountedMonthStillReportsZeroRatherThanNegative() {
    usedThisMonth(5);

    assertThat(service.allowance(PASSENGER).remaining()).isZero();
  }
}
