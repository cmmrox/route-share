package com.routeshare.storage.dto;

import java.util.Map;

/**
 * Presigned upload instructions returned to the client. The client PUTs the file bytes to {@code
 * uploadUrl} with the given {@code headers}, then calls the matching submit endpoint with {@code
 * documentId} to move the document into review.
 */
public record UploadUrlResponse(
    long documentId,
    String storageKey,
    String uploadUrl,
    String httpMethod,
    Map<String, String> headers,
    long expiresInSeconds) {}
