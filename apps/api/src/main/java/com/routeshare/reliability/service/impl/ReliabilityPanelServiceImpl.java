package com.routeshare.reliability.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.reliability.domain.ReliabilityEventType;
import com.routeshare.reliability.domain.ReliabilityRole;
import com.routeshare.reliability.dto.response.DriverReliabilityResponse;
import com.routeshare.reliability.dto.response.PassengerReliabilityResponse;
import com.routeshare.reliability.entity.MonthlyCounterEntity;
import com.routeshare.reliability.repository.MonthlyCounterRepository;
import com.routeshare.reliability.repository.ReliabilityEventRepository;
import com.routeshare.reliability.service.ReliabilityPanelService;
import com.routeshare.reliability.service.ReliabilityService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReliabilityPanelServiceImpl implements ReliabilityPanelService {

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final ReliabilityService reliability;
  private final ReliabilityEventRepository events;
  private final MonthlyCounterRepository counters;
  private final PolicySettingService policy;
  private final com.routeshare.driver.facade.DriverFacade drivers;
  private final Clock clock;

  @Override
  @Transactional
  public DriverReliabilityResponse driverPanel() {
    long appUserId = currentAppUserId();
    LocalDate period = reliability.currentPeriod();
    MonthlyCounterEntity counter = reliability.counter(appUserId, ReliabilityRole.DRIVER, period);

    int missedStartLimit = policy.integer(PolicyKey.MISSED_START_LIMIT);
    boolean deactivated = drivers.isDeactivated(appUserId);

    // D34 shows the misses themselves, not just how many. The log is the only thing that can
    // answer "which three?", which is the question a driver actually asks.
    var detail =
        events
            .findByAppUserIdAndRoleAndEventTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
                appUserId,
                ReliabilityRole.DRIVER,
                ReliabilityEventType.MISSED_START,
                startOf(period),
                startOf(period.plusMonths(1)))
            .stream()
            .map(
                e ->
                    new DriverReliabilityResponse.Occurrence(
                        e.getOccurredAt(), e.getTripId(), e.getMetadata()))
            .toList();

    return new DriverReliabilityResponse(
        period,
        new DriverReliabilityResponse.Counted(counter.getMissedStarts(), missedStartLimit),
        new DriverReliabilityResponse.Counted(counter.getLateCancellations(), missedStartLimit),
        counter.getStartExtensionsUsed(),
        pct(counter.getOnTimeEvents(), counter.getOnTimeOpportunities()),
        pct(counter.getTripsCompleted(), counter.getTripsBooked()),
        new DriverReliabilityResponse.DeactivationRisk(
            Math.max(0, missedStartLimit - counter.getMissedStarts()), deactivated),
        detail);
  }

  @Override
  @Transactional
  public PassengerReliabilityResponse passengerPanel() {
    long appUserId = currentAppUserId();
    LocalDate period = reliability.currentPeriod();
    MonthlyCounterEntity counter =
        reliability.counter(appUserId, ReliabilityRole.PASSENGER, period);
    int prepayThreshold = policy.integer(PolicyKey.PAX_PREPAY_NO_SHOW_THRESHOLD);

    return new PassengerReliabilityResponse(
        period,
        pct(counter.getTripsCompleted(), counter.getTripsBooked()),
        new PassengerReliabilityResponse.NoShows(counter.getNoShows(), prepayThreshold),
        counter.getLateCancels(),
        pct(counter.getOnTimeEvents(), counter.getOnTimeOpportunities()),
        counter.getNoShows() >= prepayThreshold);
  }

  @Override
  @Transactional
  public int rolloverMonth() {
    LocalDate period = reliability.currentPeriod();
    // Opening the new month's rows is all a "reset" is. The previous month's row is untouched, so
    // a rider asking in March why she is prepaying can still be shown February.
    int opened = 0;
    for (var previous : counters.findByPeriodMonth(period.minusMonths(1))) {
      if (counters
          .findByAppUserIdAndRoleAndPeriodMonth(previous.getAppUserId(), previous.getRole(), period)
          .isEmpty()) {
        counters.save(
            MonthlyCounterEntity.opened(previous.getAppUserId(), previous.getRole(), period));
        opened++;
      }
    }
    return opened;
  }

  /**
   * A percentage with no opportunities behind it is not zero, it is unknown — and showing a driver
   * "0% on time" on his first day would be a lie the panel told itself.
   */
  private BigDecimal pct(int numerator, int denominator) {
    if (denominator <= 0) {
      return null;
    }
    return BigDecimal.valueOf(numerator)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
  }

  private Instant startOf(LocalDate month) {
    return month.atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  private long currentAppUserId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }
}
