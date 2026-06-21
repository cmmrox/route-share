package com.routeshare.booking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request to create a trip share-link. {@code expiresInMinutes} is optional (server default
 * applies); {@code notifyContacts} sends the link to the passenger's trusted contacts by SMS.
 */
public record ShareTripRequest(
    @Min(5) @Max(1440) Integer expiresInMinutes, Boolean notifyContacts) {}
