package com.routeshare.admin.dto;

public record AdminVehicleResponse(
    long id,
    long driverProfileId,
    String make,
    String model,
    Integer manufactureYear,
    String color,
    String registrationNumber,
    Integer seatCount,
    String status) {}
