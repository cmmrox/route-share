package com.routeshare.passenger.dto.response;

import java.util.List;

/** Passenger verification readiness derived from uploaded documents (verification is optional). */
public record PassengerVerificationStatusResponse(
    String status, boolean required, List<PassengerDocumentResponse> documents) {}
