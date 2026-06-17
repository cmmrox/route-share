package com.routeshare.passenger.dto.response;

import java.time.Instant;

public record PassengerDocumentResponse(
    long id,
    String documentType,
    String status,
    String contentType,
    Long fileSizeBytes,
    String originalFilename,
    String rejectionReason,
    Instant submittedAt,
    Instant reviewedAt,
    Instant createdAt) {}
