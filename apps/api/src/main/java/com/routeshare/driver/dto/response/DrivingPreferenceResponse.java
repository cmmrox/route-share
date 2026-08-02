package com.routeshare.driver.dto.response;

import java.time.Instant;

/**
 * D35, with the one fact the screen cannot work out for itself: whether women-only is even offered
 * to this driver.
 *
 * <p>{@code canSetWomenOnly} is false for every driver whose NIC has not verified her as female,
 * and the toggle is hidden rather than shown and refused — a control that always fails is worse
 * than no control.
 */
public record DrivingPreferenceResponse(
    String genderPolicy,
    boolean verifiedRidersOnly,
    boolean approveEachRequest,
    boolean midTripBookings,
    boolean earlyDropRequests,
    boolean chatEnabled,
    boolean canSetWomenOnly,
    Instant updatedAt) {}
