package com.routeshare.driver.facade;

import com.routeshare.driver.domain.DriverGate;
import java.util.List;
import java.util.Optional;

public interface DriverFacade {
  Optional<Long> findDriverProfileIdByAppUserId(long appUserId);

  Optional<Long> findApprovedDriverProfileIdByAppUserId(long appUserId);

  Optional<String> findDriverStatusByAppUserId(long appUserId);

  /** The account behind a driver profile — for notifying the driver from another module. */
  Optional<Long> findAppUserIdByDriverProfileId(long driverProfileId);

  /**
   * Why this account cannot use driver endpoints — empty when it can. The single seam other modules
   * use, so no one reaches into driver repositories to ask.
   */
  List<DriverGate> gatesFor(long appUserId);

  /** Why this driver cannot publish a route — empty when they can. */
  List<DriverGate> publishGatesFor(long appUserId);

  /** True while a driver deactivation is open. Riding and payouts are unaffected by it. */
  boolean isDeactivated(long appUserId);

  /**
   * Slice 05's automatic trigger at the missed-start limit.
   *
   * <p>Deactivation stops <em>driving</em>. The same person keeps booking rides and money already
   * earned still pays out — that is the whole distinction between this and a suspension, and it is
   * why this goes through the driver module rather than touching the account.
   *
   * @return the case reference the driver is given, or empty if they have no driver profile
   */
  java.util.Optional<String> deactivateForMissedStarts(long appUserId, int missedStarts);
}
