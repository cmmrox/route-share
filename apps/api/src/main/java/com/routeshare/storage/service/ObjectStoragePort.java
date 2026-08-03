package com.routeshare.storage.service;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * Abstraction over private object storage for KYC/vehicle/profile documents. Bytes never flow
 * through the API: clients upload and download directly via short-lived presigned URLs, and the
 * backend only stores metadata + the storage key. This keeps large binaries off the app servers and
 * lets storage authorization stay time-boxed.
 */
public interface ObjectStoragePort {

  /** A presigned PUT the client uses to upload bytes directly to storage. */
  record PresignedUpload(
      String storageKey, URI url, String httpMethod, Map<String, String> headers) {}

  /**
   * Creates a presigned upload for {@code storageKey}. The client must send the returned {@code
   * headers} (notably Content-Type) verbatim or the upload is rejected by storage.
   */
  PresignedUpload createUploadUrl(String storageKey, String contentType, Duration ttl);

  /** Creates a short-lived presigned GET so an authorized caller can read a private object. */
  URI createDownloadUrl(String storageKey, Duration ttl);

  /**
   * Returns true if the object exists — used to confirm an upload actually landed before submit.
   */
  boolean exists(String storageKey);

  /** Reads only the first bytes needed for server-side file signature validation. */
  byte[] readPrefix(String storageKey, int maxBytes);

  /** Deletes an object (e.g. when a document is replaced or a record is purged). */
  void delete(String storageKey);
}
