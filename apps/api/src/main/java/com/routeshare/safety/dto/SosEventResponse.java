package com.routeshare.safety.dto;

import java.time.Instant;

public record SosEventResponse(
    long id,
    String status,
    Long tripId,
    Long bookingId,
    Double latitude,
    Double longitude,
    String role,
    String vehicleRegistration,
    String destinationLabel,
    String placeLabel,
    int contactsAlerted,
    int contactAlertFailures,
    String note,
    Instant createdAt) {}
