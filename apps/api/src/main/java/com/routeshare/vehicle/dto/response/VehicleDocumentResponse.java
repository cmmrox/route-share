package com.routeshare.vehicle.dto.response;

import java.time.Instant;

public record VehicleDocumentResponse(
    long id,
    long vehicleId,
    String documentType,
    String storageKey,
    String status,
    String rejectionReason,
    Instant createdAt) {}
