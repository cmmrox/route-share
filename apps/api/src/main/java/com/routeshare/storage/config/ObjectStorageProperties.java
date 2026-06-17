package com.routeshare.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the S3-compatible document store (MinIO locally, S3/R2 in production). When
 * {@code enabled} is false the {@link
 * com.routeshare.storage.service.impl.DisabledObjectStorageAdapter} is active and document upload
 * endpoints return {@code 412 Precondition Failed} instead of faking success.
 */
@ConfigurationProperties(prefix = "routeshare.object-storage")
public record ObjectStorageProperties(
    boolean enabled,
    String endpoint,
    String region,
    String bucket,
    String accessKey,
    String secretKey,
    Boolean pathStyle,
    Integer presignTtlSeconds) {
  public ObjectStorageProperties {
    region = region == null || region.isBlank() ? "us-east-1" : region;
    pathStyle = pathStyle == null || pathStyle;
    presignTtlSeconds =
        presignTtlSeconds == null || presignTtlSeconds <= 0 ? 900 : presignTtlSeconds;
  }

  public boolean ready() {
    return enabled
        && endpoint != null
        && !endpoint.isBlank()
        && bucket != null
        && !bucket.isBlank()
        && accessKey != null
        && !accessKey.isBlank()
        && secretKey != null
        && !secretKey.isBlank();
  }
}
