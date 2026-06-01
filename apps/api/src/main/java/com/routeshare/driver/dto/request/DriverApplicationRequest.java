package com.routeshare.driver.dto.request;

import jakarta.validation.constraints.*;

public record DriverApplicationRequest(@NotBlank String displayName) {}
