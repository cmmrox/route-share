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
   * D35's set gate, for the app shell: whether this account may even be offered the women-only
   * toggle. A control that always fails is worse than no control, so the screen hides it.
   */
  boolean canSetWomenOnly(long appUserId);

  /**
   * The eligibility and approval defaults a newly generated occurrence inherits.
   *
   * <p>Copied onto the trip rather than read through at query time: changing a preference must not
   * silently change the terms of a trip somebody has already booked.
   */
  TripDefaults tripDefaultsFor(long driverProfileId);

  /** Written by KYC review from the NIC. Self-declaration would defeat the point of the gate. */
  void recordGender(long driverProfileId, String gender);

  record TripDefaults(String genderPolicy, boolean verifiedRidersOnly, boolean approveEachRequest) {
    /** What an occurrence gets when its driver has never opened the preferences screen. */
    public static TripDefaults cautious() {
      return new TripDefaults("ANYONE", false, true);
    }
  }
}
