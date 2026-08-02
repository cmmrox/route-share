package com.routeshare.driver.facade.impl;

import com.routeshare.driver.facade.DriverDeactivationFacade;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.driver.service.DriverDeactivationService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DriverDeactivationFacadeImpl implements DriverDeactivationFacade {

  private final DriverProfileRepository drivers;
  private final DriverDeactivationService deactivations;
  private final com.routeshare.routing.facade.RoutingFacade routing;

  @Override
  @Transactional
  public Optional<String> deactivateForMissedStarts(long appUserId, int missedStarts) {
    return drivers
        .findByAppUserId(appUserId)
        .map(
            driver -> {
              String caseRef =
                  deactivations
                      .deactivateAutomatically(
                          driver.getId(),
                          "Missed " + missedStarts + " trip starts this month",
                          "AUTO-MISSED-START-" + driver.getId() + "-" + missedStarts)
                      .caseRef();
              // His future offers come down with him. Left published, riders would keep booking
              // seats in a car that is no longer allowed to carry them, and the first anybody
              // would hear of it is at the kerb.
              routing.cancelFutureOccurrencesForDriver(driver.getId());
              return caseRef;
            });
  }
}
