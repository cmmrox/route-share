package com.routeshare.rating.dto;

import java.time.Instant;

public record RatingResponse(
    long id, long bookingId, int stars, String comment, Instant createdAt) {}
