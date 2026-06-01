package com.routeshare.passenger.dto.response;

import java.util.Map;

public record PassengerProfileResponse(
    long id, String fullName, String photoUrl, Map<String, Object> preferences) {}
