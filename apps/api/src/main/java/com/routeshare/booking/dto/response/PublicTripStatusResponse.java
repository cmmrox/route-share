package com.routeshare.booking.dto.response;

import java.time.Instant;

/** Non-sensitive live trip status returned to anyone holding a valid share token. */
public record PublicTripStatusResponse(
    String originLabel,
    String destinationLabel,
    Instant departureTime,
    String bookingStatus,
    String tripStatus,
    String passengerTripStatus,
    String driverName,
    String vehiclePlate,
    Instant expiresAt) {}
