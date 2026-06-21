package com.routeshare.passenger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Upload metadata for a passenger document (avatar / identity); document type is fixed by endpoint.
 */
public record PassengerDocumentUploadRequest(
    @NotBlank @Size(max = 120) String contentType,
    @Positive long fileSizeBytes,
    @Size(max = 255) String originalFilename) {}
