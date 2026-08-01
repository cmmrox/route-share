package com.routeshare.driver.facade;

import com.routeshare.driver.domain.DriverGate;
import java.util.List;
import java.util.Optional;

public interface DriverFacade {
  Optional<Long> findDriverProfileIdByAppUserId(long appUserId);

  Optional<Long> findApprovedDriverProfileIdByAppUserId(long appUserId);

  Optional<String> findDriverStatusByAppUserId(long appUserId);

  /**
   * Why this account cannot use driver endpoints — empty when it can. The single seam other modules
   * use, so no one reaches into driver repositories to ask.
   */
  List<DriverGate> gatesFor(long appUserId);

  /** Why this driver cannot publish a route — empty when they can. */
  List<DriverGate> publishGatesFor(long appUserId);

  /** True while a driver deactivation is open. Riding and payouts are unaffected by it. */
  boolean isDeactivated(long appUserId);
}
