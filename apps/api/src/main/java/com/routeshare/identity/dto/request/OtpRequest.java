package com.routeshare.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpRequest(@NotBlank @Size(max = 32) String phoneNumber) {}
