package com.routeshare.vehicle.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RateBandReviewRequestCommand(
    @NotBlank @Size(max = 120) String reason, @Size(max = 2000) String note) {}
