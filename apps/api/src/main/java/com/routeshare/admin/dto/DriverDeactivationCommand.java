package com.routeshare.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param reason shown to the driver on D34 — user-safe wording only, no internal notes
 * @param caseRef the reference the driver quotes when they appeal
 */
public record DriverDeactivationCommand(
    @NotBlank @Size(max = 500) String reason, @NotBlank @Size(max = 60) String caseRef) {}
