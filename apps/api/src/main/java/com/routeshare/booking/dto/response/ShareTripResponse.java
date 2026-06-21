package com.routeshare.booking.dto.response;

import java.time.Instant;

public record ShareTripResponse(
    String token, String shareUrl, Instant expiresAt, int contactsNotified) {}
