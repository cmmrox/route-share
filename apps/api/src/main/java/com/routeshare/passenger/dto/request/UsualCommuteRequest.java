package com.routeshare.passenger.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** P02. Null clears the saved commute rather than needing a second endpoint to delete it. */
public record UsualCommuteRequest(
    @NotBlank @Size(max = 200) String originLabel,
    @NotNull @Valid com.routeshare.routing.dto.request.CoordinateRequest origin,
    @NotBlank @Size(max = 200) String destinationLabel,
    @NotNull @Valid com.routeshare.routing.dto.request.CoordinateRequest destination,
    /** "08:15". The time she usually leaves, which is what makes the match count meaningful. */
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String habitualTime) {}
