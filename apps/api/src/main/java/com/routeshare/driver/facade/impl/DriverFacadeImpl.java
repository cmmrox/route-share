package com.routeshare.driver.facade.impl;

import com.routeshare.driver.domain.DriverGate;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.driver.service.DriverGateService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DriverFacadeImpl implements DriverFacade {
  private final DriverProfileRepository drivers;
  private final DriverGateService gates;

  @Override
  public Optional<Long> findDriverProfileIdByAppUserId(long appUserId) {
    return drivers.findIdByAppUserId(appUserId);
  }

  @Override
  public Optional<Long> findApprovedDriverProfileIdByAppUserId(long appUserId) {
    return drivers.findApprovedIdByAppUserId(appUserId);
  }

  @Override
  public Optional<String> findDriverStatusByAppUserId(long appUserId) {
    return drivers.findStatusByAppUserId(appUserId);
  }

  @Override
  public List<DriverGate> gatesFor(long appUserId) {
    return gates.driveGates(appUserId);
  }

  @Override
  public List<DriverGate> publishGatesFor(long appUserId) {
    return gates.publishGates(appUserId);
  }

  @Override
  public boolean isDeactivated(long appUserId) {
    return gates.isDeactivated(appUserId);
  }
}
