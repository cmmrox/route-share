package com.routeshare.vehicle.dto.response;

import java.math.BigDecimal;

/** Drives D07's class picker: the seat options offered and the range a band will sit inside. */
public record VehicleClassResponse(
    String classKey,
    String label,
    int maxPassengerSeats,
    BigDecimal defaultRateMin,
    BigDecimal defaultRateMax) {}
