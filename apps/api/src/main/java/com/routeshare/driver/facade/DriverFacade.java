package com.routeshare.driver.facade;

import java.util.Optional;

public interface DriverFacade {
  Optional<Long> findDriverProfileIdByAppUserId(long appUserId);

  Optional<Long> findApprovedDriverProfileIdByAppUserId(long appUserId);

  Optional<String> findDriverStatusByAppUserId(long appUserId);
}
