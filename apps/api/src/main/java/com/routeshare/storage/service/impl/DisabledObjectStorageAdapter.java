package com.routeshare.storage.service.impl;

import com.routeshare.storage.service.ObjectStoragePort;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fallback used when object storage is not configured. It fails closed — document upload/download
 * endpoints return {@code 412 Precondition Failed} rather than pretending an upload succeeded. This
 * keeps the contract honest in environments without storage credentials.
 */
@Component
@ConditionalOnProperty(
    prefix = "routeshare.object-storage",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class DisabledObjectStorageAdapter implements ObjectStoragePort {
  private static ResponseStatusException notConfigured() {
    return new ResponseStatusException(
        HttpStatus.PRECONDITION_FAILED,
        "Document storage is not configured. Set routeshare.object-storage.* to enable uploads.");
  }

  @Override
  public PresignedUpload createUploadUrl(String storageKey, String contentType, Duration ttl) {
    throw notConfigured();
  }

  @Override
  public URI createDownloadUrl(String storageKey, Duration ttl) {
    throw notConfigured();
  }

  @Override
  public boolean exists(String storageKey) {
    throw notConfigured();
  }

  @Override
  public void delete(String storageKey) {
    throw notConfigured();
  }
}
