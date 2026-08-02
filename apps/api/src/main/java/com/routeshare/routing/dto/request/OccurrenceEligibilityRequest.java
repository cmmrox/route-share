package com.routeshare.routing.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** D13 — who can ride on this one trip, overriding D35's standing answer. */
public record OccurrenceEligibilityRequest(
    @NotNull @Pattern(regexp = "ANYONE|WOMEN_ONLY") String genderPolicy,
    boolean verifiedRidersOnly) {}
