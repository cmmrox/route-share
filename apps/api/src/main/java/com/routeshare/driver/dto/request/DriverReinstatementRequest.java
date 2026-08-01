package com.routeshare.driver.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DriverReinstatementRequest(@NotBlank @Size(max = 2000) String message) {}
