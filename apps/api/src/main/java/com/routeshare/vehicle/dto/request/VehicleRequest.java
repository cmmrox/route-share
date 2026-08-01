package com.routeshare.vehicle.dto.request;

import jakarta.validation.constraints.*;

/**
 * @param seatCount seats offered to riders, capped by the class (D07)
 * @param vehicleClass one of {@code vehicle.vehicle_class}; drives the seat cap and the band range
 */
public record VehicleRequest(
    @NotBlank String make,
    @NotBlank String model,
    @Min(1980) @Max(2100) int manufactureYear,
    @NotBlank String color,
    @NotBlank String registrationNumber,
    @Min(1) @Max(12) int seatCount,
    @NotBlank String vehicleClass) {}
