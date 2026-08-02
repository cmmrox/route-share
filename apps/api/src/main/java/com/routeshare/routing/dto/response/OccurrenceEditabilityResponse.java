package com.routeshare.routing.dto.response;

/**
 * D09's banner. {@code editable} is computed from the occurrence's status and its live seat holds,
 * never stored — see {@link com.routeshare.routing.domain.TripEditability}.
 */
public record OccurrenceEditabilityResponse(
    long routeOccurrenceId, boolean editable, int bookedSeats, String reason) {}
