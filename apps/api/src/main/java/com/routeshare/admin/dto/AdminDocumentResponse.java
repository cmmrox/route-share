package com.routeshare.admin.dto;

import java.time.Instant;

public record AdminDocumentResponse(
    long id,
    String scope,
    String documentType,
    String status,
    String rejectionReason,
    Instant reviewedAt) {}
