package com.routeshare.vehicle.dto.response;

import java.time.Instant;

public record VehicleDocumentResponse(
    long id,
    long vehicleId,
    String documentType,
    String status,
    String contentType,
    Long fileSizeBytes,
    String originalFilename,
    String rejectionReason,
    Instant submittedAt,
    Instant reviewedAt,
    Instant createdAt) {}
