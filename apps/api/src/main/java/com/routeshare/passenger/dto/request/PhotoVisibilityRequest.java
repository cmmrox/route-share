package com.routeshare.passenger.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** P30. */
public record PhotoVisibilityRequest(
    @NotNull @Pattern(regexp = "PUBLIC|MATCHED|HIDDEN") String visibility) {}
