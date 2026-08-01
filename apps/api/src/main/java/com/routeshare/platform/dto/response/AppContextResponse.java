package com.routeshare.platform.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Everything the ComiGo app shell needs on cold start and after a mode switch, in one call.
 *
 * <p>Screens S07–S14 collectively need available modes, the driver gate and its reason,
 * verification level, suspension detail, an active-trip pointer for the resume bar, outstanding
 * dues, the rewards balance and tab badges. Served separately that is eight or more calls on every
 * launch.
 *
 * <p>Fields owned by later slices of the ComiGo backend plan return safe zero values until those
 * slices land, so the mobile shell never has to be reshaped as they arrive.
 */
public record AppContextResponse(
    String subject,
    String displayName,
    String phone,
    String email,
    String photoUrl,
    Set<String> availableModes,
    String activeModeDefault,
    Driver driver,
    Passenger passenger,
    Account account,
    ActiveTrip activeTrip,
    Money money,
    Badges badges,
    Settings settings,
    Instant serverTime) {

  /**
   * A reason the caller cannot do something, expressed as data the app renders rather than an
   * opaque 403. Drives S07 (no driver access), S08 (pending), S09 (rejected), S12 (publish gate),
   * S13 (suspended), D34 (deactivated) and D40 (no rate band).
   */
  public record Gate(String code, String message, String actionPath) {}

  public record Driver(
      String status, List<Gate> gates, boolean canPublish, boolean canSetWomenOnly) {}

  public record Passenger(
      String verificationLevel, String photoVisibility, boolean prepayRequired) {}

  public record Account(String status, String reason, String caseRef, Instant suspendedAt) {}

  /** Drives the resume bar on P01b and D08b, and the mode-switch sheets S10 and S11. */
  public record ActiveTrip(
      String kind, Long bookingId, Long tripId, Integer etaMinutes, String label) {}

  public record Money(String currency, BigDecimal outstandingDues, BigDecimal rewardsBalance) {}

  /**
   * Tab badge rules from board S14: Home and Account are dots only and never counts; Trips and
   * Inbox are counts; the centre action tab is never badged.
   */
  public record Badges(boolean home, int trips, int inbox, boolean account) {}

  public record Settings(String theme, String language) {}
}
