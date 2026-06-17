package com.routeshare.maps.dto;

public record PlaceSuggestionResponse(
    String placeId, String label, String address, CoordinateResponse coordinate) {}
