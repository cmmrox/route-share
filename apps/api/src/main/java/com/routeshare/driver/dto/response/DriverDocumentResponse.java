package com.routeshare.driver.dto.response;

import java.time.Instant;

public record DriverDocumentResponse(
    long id,
    String documentType,
    String storageKey,
    String status,
    String rejectionReason,
    Instant createdAt) {}
