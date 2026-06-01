package com.routeshare.passenger.dto.response;

public record SavedPlaceResponse(
    long id, String label, String address, double latitude, double longitude) {}
