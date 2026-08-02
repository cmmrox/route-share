package com.routeshare.routing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A curated landmark. This is the only tier with a real name in it, which is why an operator types
 * the description and the side hint rather than a geocoder inventing them.
 */
public record PickupPointRequest(
    @NotBlank @Size(max = 200) String label,
    @Size(max = 300) String description,
    /** "Kerb side, opposite the pharmacy" — the half a coordinate cannot carry. */
    @Size(max = 200) String sideHint,
    @NotNull @Valid CoordinateRequest position) {}
