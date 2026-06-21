package com.routeshare.driver.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Upload metadata for a KYC document (identity / licence); the document type is fixed by endpoint.
 */
public record DriverKycUploadRequest(
    @NotBlank @Size(max = 120) String contentType,
    @Positive long fileSizeBytes,
    @Size(max = 255) String originalFilename) {}
