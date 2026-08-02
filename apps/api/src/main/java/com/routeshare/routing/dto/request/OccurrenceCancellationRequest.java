package com.routeshare.routing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * D30's reason picker. The code is required — "cancelled" with no reason is the one thing the rider
 * notification cannot explain, and it is also what a repeat-cancellation review has to read.
 *
 * <p>The note is free text from a driver and is treated as untrusted: length-limited here, stored
 * as text, and never rendered as markup on an admin surface.
 */
public record OccurrenceCancellationRequest(
    @NotBlank @Pattern(regexp = "VEHICLE_PROBLEM|UNWELL|PLANS_CHANGED|WRONG_DETAILS|OTHER")
        String reasonCode,
    @Size(max = 2000) String note) {}
