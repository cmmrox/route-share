package com.routeshare.reliability.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.driver.facade.DriverDeactivationFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.reliability.domain.ReliabilityEventType;
import com.routeshare.reliability.domain.ReliabilityRole;
import com.routeshare.reliability.entity.MonthlyCounterEntity;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 05-18. Three missed starts stops driving — and only driving.
 *
 * <p>The trigger lives with the counter rather than with the auto-cancel that happened to record
 * the third miss, so a missed start recorded from anywhere fires the same rule. A rule enforced in
 * one caller is only as reliable as the last place somebody remembered it.
 */
class ReliabilityGateServiceImplTest {
  private static final LocalDate AUGUST = LocalDate.of(2026, 8, 1);
  private static final long DRIVER = 9L;

  private final DriverDeactivationFacade drivers = mock(DriverDeactivationFacade.class);
  private final PolicySettingService policy = mock(PolicySettingService.class);
  private final NotificationFacade notifications = mock(NotificationFacade.class);
  private final ReliabilityGateServiceImpl service =
      new ReliabilityGateServiceImpl(drivers, policy, notifications, new SimpleMeterRegistry());

  private MonthlyCounterEntity withMissedStarts(int missed) {
    when(policy.integer(PolicyKey.MISSED_START_LIMIT)).thenReturn(3);
    var counter = MonthlyCounterEntity.opened(DRIVER, ReliabilityRole.DRIVER, AUGUST);
    counter.setMissedStarts(missed);
    return counter;
  }

  @Test
  void twoMissedStartsDoNotDeactivate() {
    service.onCounterChanged(
        DRIVER, ReliabilityRole.DRIVER, ReliabilityEventType.MISSED_START, withMissedStarts(2));

    verify(drivers, never()).deactivateForMissedStarts(anyLong(), anyInt());
  }

  @Test
  void theThirdMissedStartDeactivatesDrivingAndTellsHim() {
    when(drivers.deactivateForMissedStarts(DRIVER, 3)).thenReturn(Optional.of("AUTO-CASE-1"));

    service.onCounterChanged(
        DRIVER, ReliabilityRole.DRIVER, ReliabilityEventType.MISSED_START, withMissedStarts(3));

    verify(drivers).deactivateForMissedStarts(DRIVER, 3);
    verify(notifications)
        .notifyUser(
            org.mockito.ArgumentMatchers.eq(DRIVER),
            org.mockito.ArgumentMatchers.eq("DRIVER_DEACTIVATED"),
            any(),
            // The message has to say riding and payouts are unaffected, because that is the first
            // thing a driver who has just lost driving will assume he has also lost.
            org.mockito.ArgumentMatchers.contains("still book rides"),
            any());
  }

  /** A passenger's no-shows must never reach the driver deactivation path. */
  @Test
  void aPassengerNoShowNeverDeactivatesAnybody() {
    service.onCounterChanged(
        DRIVER, ReliabilityRole.PASSENGER, ReliabilityEventType.NO_SHOW, withMissedStarts(3));

    verify(drivers, never()).deactivateForMissedStarts(anyLong(), anyInt());
  }

  /** Some other driver event at the limit is not a missed start and must not deactivate. */
  @Test
  void anExtensionUsedAtTheLimitDoesNotDeactivate() {
    service.onCounterChanged(
        DRIVER,
        ReliabilityRole.DRIVER,
        ReliabilityEventType.START_EXTENSION_USED,
        withMissedStarts(3));

    verify(drivers, never()).deactivateForMissedStarts(anyLong(), anyInt());
  }

  /** No driver profile behind the account: nothing to deactivate, and no notification either. */
  @Test
  void anAccountWithNoDriverProfileIsNotNotified() {
    when(drivers.deactivateForMissedStarts(DRIVER, 3)).thenReturn(Optional.empty());

    service.onCounterChanged(
        DRIVER, ReliabilityRole.DRIVER, ReliabilityEventType.MISSED_START, withMissedStarts(3));

    verify(notifications, never()).notifyUser(anyLong(), any(), any(), any(), any());
  }
}
