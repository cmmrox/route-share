package com.routeshare.routing.dto.response;

/** D14's share card: the code, the link a rider taps, and where to fetch the QR image. */
public record TripShareCodeResponse(
    long routeOccurrenceId, String shortCode, String shortUrl, String qrPngUrl, boolean revoked) {}
