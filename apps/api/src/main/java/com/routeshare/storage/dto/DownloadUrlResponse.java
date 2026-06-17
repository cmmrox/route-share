package com.routeshare.storage.dto;

/** Short-lived presigned download URL for a private document. */
public record DownloadUrlResponse(String downloadUrl, long expiresInSeconds) {}
