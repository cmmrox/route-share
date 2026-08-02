package com.routeshare.routing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** D13: who can book this trip. */
public record ApprovalModeRequest(
    @NotBlank @Pattern(regexp = "INSTANT|APPROVE_EACH") String mode) {}
