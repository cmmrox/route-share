package com.routeshare.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Request for a presigned upload URL. Shared by driver, vehicle, and passenger document flows. */
public record UploadUrlRequest(
    @NotBlank @Size(max = 80) String documentType,
    @NotBlank @Size(max = 120) String contentType,
    @Positive long fileSizeBytes,
    @Size(max = 255) String originalFilename) {}
