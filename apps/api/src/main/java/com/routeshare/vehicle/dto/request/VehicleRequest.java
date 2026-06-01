package com.routeshare.vehicle.dto.request;

import jakarta.validation.constraints.*;

public record VehicleRequest(
    @NotBlank String make,
    @NotBlank String model,
    @Min(1980) @Max(2100) int manufactureYear,
    @NotBlank String color,
    @NotBlank String registrationNumber,
    @Min(1) @Max(12) int seatCount) {}
