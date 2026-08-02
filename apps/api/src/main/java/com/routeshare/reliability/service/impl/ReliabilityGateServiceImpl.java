package com.routeshare.reliability.service.impl;

import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.reliability.domain.ReliabilityEventType;
import com.routeshare.reliability.domain.ReliabilityRole;
import com.routeshare.reliability.entity.MonthlyCounterEntity;
import com.routeshare.reliability.service.ReliabilityGateService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReliabilityGateServiceImpl implements ReliabilityGateService {
  private static final Logger log = LoggerFactory.getLogger(ReliabilityGateServiceImpl.class);

  private final DriverFacade drivers;
  private final PolicySettingService policy;
  private final NotificationFacade notifications;
  private final MeterRegistry meters;

  @Override
  public void onCounterChanged(
      long appUserId,
      ReliabilityRole role,
      ReliabilityEventType type,
      MonthlyCounterEntity counter) {
    if (role == ReliabilityRole.DRIVER && type == ReliabilityEventType.MISSED_START) {
      deactivateAtLimit(appUserId, counter);
    }
    // The passenger prepay flag needs no action here: it is read from this same counter by
    // /me/context, so there is one number and no flag to fall out of step with it.
  }

  private void deactivateAtLimit(long appUserId, MonthlyCounterEntity counter) {
    int limit = policy.integer(PolicyKey.MISSED_START_LIMIT);
    if (counter.getMissedStarts() < limit) {
      return;
    }
    // Idempotent below: a driver already deactivated keeps their original case reference, so the
    // fourth miss does not issue a second case or send a second notification's worth of alarm.
    drivers
        .deactivateForMissedStarts(appUserId, counter.getMissedStarts())
        .ifPresent(
            caseRef -> {
              notifications.notifyUser(
                  appUserId,
                  "DRIVER_DEACTIVATED",
                  "Driving has been paused on your account",
                  "You have missed "
                      + counter.getMissedStarts()
                      + " trip starts this month. You can still book rides, and any pending payout"
                      + " is unaffected.",
                  Map.of("caseRef", caseRef));
              meters.counter("routeshare_driver_deactivations_total").increment();
              log.info(
                  "driver {} deactivated automatically after {} missed starts (case {})",
                  appUserId,
                  counter.getMissedStarts(),
                  caseRef);
            });
  }
}
