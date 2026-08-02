package com.routeshare.passenger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * P29 — one capture, attested at the moment it is taken.
 *
 * <p>{@code captureSource} and {@code capturedAt} are the attestation, and {@code sessionId} binds
 * the image to the attempt a reviewer will see it in. This is deterrence rather than proof: a
 * determined client can lie about all three, which is exactly why the review step stays human and
 * why the values are recorded for the reviewer rather than merely checked and discarded.
 */
public record VerificationCaptureUploadRequest(
    @NotNull Long sessionId,
    @NotBlank @Size(max = 20) String captureSource,
    @NotNull Instant capturedAt,
    @NotBlank @Size(max = 120) String contentType,
    @Positive long fileSizeBytes,
    @Size(max = 255) String originalFilename) {}
