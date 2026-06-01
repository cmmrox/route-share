package com.routeshare.vehicle.dto.response;

public record VehicleResponse(
    long id,
    String make,
    String model,
    int manufactureYear,
    String color,
    String registrationNumber,
    int seatCount,
    String status) {}
