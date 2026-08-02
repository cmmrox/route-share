package com.routeshare.driver.facade.impl;

import com.routeshare.driver.domain.DriverGate;
import com.routeshare.driver.entity.DriverProfileEntity;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.driver.repository.DrivingPreferenceRepository;
import com.routeshare.driver.service.DriverGateService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DriverFacadeImpl implements DriverFacade {
  private final DriverProfileRepository drivers;
  private final DrivingPreferenceRepository preferences;
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
  public Optional<Long> findAppUserIdByDriverProfileId(long driverProfileId) {
    return drivers.findById(driverProfileId).map(DriverProfileEntity::getAppUserId);
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

  @Override
  public boolean canSetWomenOnly(long appUserId) {
    return drivers
        .findByAppUserId(appUserId)
        .map(profile -> "FEMALE".equalsIgnoreCase(profile.getGender()))
        .orElse(false);
  }

  @Override
  public TripDefaults tripDefaultsFor(long driverProfileId) {
    return preferences
        .findById(driverProfileId)
        .map(
            p ->
                new TripDefaults(
                    p.getGenderPolicy(), p.isVerifiedRidersOnly(), p.isApproveEachRequest()))
        .orElseGet(TripDefaults::cautious);
  }

  @Override
  @Transactional
  public void recordGender(long driverProfileId, String gender) {
    drivers
        .findById(driverProfileId)
        .ifPresent(
            profile -> {
              profile.setGender(gender);
              drivers.save(profile);
            });
  }
}
