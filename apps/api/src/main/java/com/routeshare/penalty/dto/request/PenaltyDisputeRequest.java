package com.routeshare.penalty.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PenaltyDisputeRequest(
    @NotBlank @Size(max = 80) String reason, @Size(max = 2000) String note) {}
