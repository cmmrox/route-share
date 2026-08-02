package com.routeshare.booking.dto.response;

import java.math.BigDecimal;

/**
 * Result of finalizing an early drop-off.
 *
 * <p>{@code fareAdjusted} is the whole of P16b. Beyond two adjusted drops in a month the seat is
 * still released and she is still out of the car — the fare simply stands. That is stated as data
 * here rather than raised as an error, because refusing somebody already at the roadside would be
 * answering a question nobody asked.
 *
 * @param fareAdjusted whether this drop was repriced on the distance actually travelled
 * @param allowanceReasonCode {@code EARLY_DROP_ALLOWANCE_EXHAUSTED} when it was not
 */
public record EarlyDropOffResponse(
    long bookingId,
    long traveledMeters,
    BigDecimal finalFare,
    String currency,
    boolean captured,
    boolean fareAdjusted,
    String allowanceReasonCode,
    int allowanceUsed,
    int allowanceRemaining,
    String status) {}
