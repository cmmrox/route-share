package com.routeshare.vehicle.dto.response;

import java.math.BigDecimal;

/**
 * @param bandStatus NOT_SET | PENDING_ASSESSMENT | ACTIVE | UNDER_REVIEW — D06 shows this per
 *     vehicle, because an approved car with no band still cannot carry a trip
 */
public record VehicleResponse(
    long id,
    String make,
    String model,
    int manufactureYear,
    String color,
    String registrationNumber,
    int seatCount,
    String status,
    String classKey,
    String bandStatus,
    BigDecimal chosenRatePerKm) {}
