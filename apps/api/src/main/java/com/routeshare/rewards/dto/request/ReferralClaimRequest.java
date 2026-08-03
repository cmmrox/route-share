package com.routeshare.rewards.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReferralClaimRequest(
    @NotBlank @Pattern(regexp = "^[A-Za-z2-9]{6,20}$") String code) {}
