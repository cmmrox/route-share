package com.routeshare.storage.domain;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Central rules for document uploads: which content types are accepted, the maximum size, and how
 * storage keys are derived. Shared by driver KYC, vehicle, and passenger document flows so the
 * constraints stay consistent and storage keys are namespaced per owner.
 */
public final class DocumentUploadPolicy {
  private DocumentUploadPolicy() {}

  public static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

  private static final Map<String, String> ALLOWED_TYPES =
      Map.of(
          "image/jpeg", "jpg",
          "image/png", "png",
          "image/webp", "webp",
          "application/pdf", "pdf");

  public static Set<String> allowedContentTypes() {
    return ALLOWED_TYPES.keySet();
  }

  /** Throws {@link InvalidUploadException} if the content type or size is not acceptable. */
  public static void validate(String contentType, long fileSizeBytes) {
    String normalized = contentType == null ? "" : contentType.trim().toLowerCase();
    if (!ALLOWED_TYPES.containsKey(normalized)) {
      throw new InvalidUploadException(
          "Unsupported content type. Allowed: " + ALLOWED_TYPES.keySet());
    }
    if (fileSizeBytes <= 0) {
      throw new InvalidUploadException("File size must be provided and positive");
    }
    if (fileSizeBytes > MAX_FILE_SIZE_BYTES) {
      throw new InvalidUploadException("File exceeds the 10 MB limit");
    }
  }

  /** Builds a namespaced, collision-free storage key, e.g. {@code driver/42/LICENCE/<uuid>.jpg}. */
  public static String storageKey(
      String scope, long ownerId, String documentType, String contentType) {
    String ext =
        ALLOWED_TYPES.getOrDefault(
            contentType == null ? "" : contentType.trim().toLowerCase(), "bin");
    String safeType =
        documentType == null ? "DOCUMENT" : documentType.replaceAll("[^A-Za-z0-9_]", "_");
    return scope + "/" + ownerId + "/" + safeType + "/" + UUID.randomUUID() + "." + ext;
  }

  /** Thrown for client-correctable upload problems; mapped to HTTP 400 by the global handler. */
  public static class InvalidUploadException extends IllegalArgumentException {
    public InvalidUploadException(String message) {
      super(message);
    }
  }
}
