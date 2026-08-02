package com.routeshare.driver.facade;

import java.util.Optional;

/**
 * The automatic deactivation seam, deliberately separate from {@link DriverFacade}.
 *
 * <p>{@code DriverFacade} is the read-and-gates seam that identity itself depends on, so hanging a
 * write here that needs {@code DriverDeactivationService} — which needs identity — closes a bean
 * cycle. Splitting the write onto its own interface breaks it structurally rather than papering
 * over it with {@code @Lazy}, which would leave the same knot for the next person to trip on.
 */
public interface DriverDeactivationFacade {

  /**
   * Slice 05's trigger at the missed-start limit.
   *
   * <p>Deactivation stops <em>driving</em>. The same person keeps booking rides and money already
   * earned still pays out — that is the whole distinction between this and a suspension.
   *
   * @return the case reference the driver is given, or empty if they have no driver profile
   */
  Optional<String> deactivateForMissedStarts(long appUserId, int missedStarts);
}
