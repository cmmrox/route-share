package com.routeshare.driver.facade.impl;

import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.driver.repository.DriverProfileRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DriverFacadeImpl implements DriverFacade {
  private final DriverProfileRepository drivers;

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
}
