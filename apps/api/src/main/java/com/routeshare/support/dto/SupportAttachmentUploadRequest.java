package com.routeshare.support.dto;

import jakarta.validation.constraints.*;

public record SupportAttachmentUploadRequest(
    @NotBlank @Size(max = 255) String filename,
    @NotBlank @Size(max = 120) String contentType,
    @Positive long sizeBytes) {}
