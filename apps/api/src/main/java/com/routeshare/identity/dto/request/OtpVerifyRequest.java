package com.routeshare.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record OtpVerifyRequest(
    @NotNull UUID verificationId,
    @NotBlank @Size(max = 32) String phoneNumber,
    @NotBlank @Pattern(regexp = "\\d{6}") String code) {}
