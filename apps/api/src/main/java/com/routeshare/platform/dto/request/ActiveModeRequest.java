package com.routeshare.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActiveModeRequest(@NotBlank @Pattern(regexp = "PASSENGER|DRIVER") String mode) {}
