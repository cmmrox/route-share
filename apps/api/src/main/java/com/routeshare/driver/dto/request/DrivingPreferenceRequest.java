package com.routeshare.driver.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * D35. All six preferences move together — a partial update would let a client that has never heard
 * of a newer preference silently reset it to its default.
 */
public record DrivingPreferenceRequest(
    @NotNull @Pattern(regexp = "ANYONE|WOMEN_ONLY") String genderPolicy,
    boolean verifiedRidersOnly,
    boolean approveEachRequest,
    boolean midTripBookings,
    boolean earlyDropRequests,
    boolean chatEnabled) {}
