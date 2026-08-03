package com.routeshare.support.dto;

import java.time.Instant;
import java.util.Map;

public record SupportAttachmentUploadResponse(
    long attachmentId,
    String uploadUrl,
    String httpMethod,
    Map<String, String> headers,
    Instant expiresAt) {}
