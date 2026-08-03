package com.routeshare.support.dto;

import java.time.Instant;

public record SupportAttachmentResponse(
    long id,
    String filename,
    String contentType,
    long sizeBytes,
    String status,
    Instant submittedAt) {}
