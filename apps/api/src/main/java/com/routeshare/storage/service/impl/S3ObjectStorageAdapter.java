package com.routeshare.storage.service.impl;

import com.routeshare.storage.config.ObjectStorageProperties;
import com.routeshare.storage.service.ObjectStoragePort;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/** Real S3-compatible adapter backed by the AWS SDK v2. Active when object storage is enabled. */
@Component
@ConditionalOnProperty(prefix = "routeshare.object-storage", name = "enabled", havingValue = "true")
public class S3ObjectStorageAdapter implements ObjectStoragePort {
  private final S3Client s3Client;
  private final S3Presigner presigner;
  private final ObjectStorageProperties props;

  public S3ObjectStorageAdapter(
      S3Client s3Client, S3Presigner presigner, ObjectStorageProperties props) {
    this.s3Client = s3Client;
    this.presigner = presigner;
    this.props = props;
  }

  @Override
  public PresignedUpload createUploadUrl(String storageKey, String contentType, Duration ttl) {
    PutObjectRequest objectRequest =
        PutObjectRequest.builder()
            .bucket(props.bucket())
            .key(storageKey)
            .contentType(contentType)
            .build();
    var presigned =
        presigner.presignPutObject(
            PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(objectRequest)
                .build());
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("Content-Type", contentType);
    return new PresignedUpload(storageKey, toUri(presigned.url()), "PUT", headers);
  }

  @Override
  public URI createDownloadUrl(String storageKey, Duration ttl) {
    var presigned =
        presigner.presignGetObject(
            GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(
                    GetObjectRequest.builder().bucket(props.bucket()).key(storageKey).build())
                .build());
    return toUri(presigned.url());
  }

  @Override
  public boolean exists(String storageKey) {
    try {
      s3Client.headObject(
          HeadObjectRequest.builder().bucket(props.bucket()).key(storageKey).build());
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    }
  }

  @Override
  public void delete(String storageKey) {
    s3Client.deleteObject(
        DeleteObjectRequest.builder().bucket(props.bucket()).key(storageKey).build());
  }

  private static URI toUri(java.net.URL url) {
    try {
      return url.toURI();
    } catch (java.net.URISyntaxException e) {
      throw new IllegalStateException("Presigner returned a malformed URL", e);
    }
  }
}
