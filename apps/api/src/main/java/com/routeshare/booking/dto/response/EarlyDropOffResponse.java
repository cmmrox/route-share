package com.routeshare.booking.dto.response;

import java.math.BigDecimal;

/** Result of finalizing an early drop-off: actual distance traveled and the recalculated fare. */
public record EarlyDropOffResponse(
    long bookingId,
    long traveledMeters,
    BigDecimal finalFare,
    String currency,
    boolean captured,
    String status) {}
