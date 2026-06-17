package com.routeshare.storage.config;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Builds the S3 client + presigner only when object storage is enabled, so the SDK is never
 * initialized in environments without storage credentials. Path-style access (default) is required
 * by MinIO; production S3/R2 typically uses virtual-host style ({@code
 * OBJECT_STORAGE_PATH_STYLE=false}).
 */
@Configuration
@ConditionalOnProperty(prefix = "routeshare.object-storage", name = "enabled", havingValue = "true")
public class ObjectStorageConfig {

  @Bean
  StaticCredentialsProvider objectStorageCredentials(ObjectStorageProperties props) {
    return StaticCredentialsProvider.create(
        AwsBasicCredentials.create(props.accessKey(), props.secretKey()));
  }

  @Bean(destroyMethod = "close")
  S3Client s3Client(ObjectStorageProperties props, StaticCredentialsProvider credentials) {
    return S3Client.builder()
        .endpointOverride(URI.create(props.endpoint()))
        .region(Region.of(props.region()))
        .credentialsProvider(credentials)
        .httpClient(UrlConnectionHttpClient.create())
        .serviceConfiguration(
            S3Configuration.builder().pathStyleAccessEnabled(props.pathStyle()).build())
        .build();
  }

  @Bean(destroyMethod = "close")
  S3Presigner s3Presigner(ObjectStorageProperties props, StaticCredentialsProvider credentials) {
    return S3Presigner.builder()
        .endpointOverride(URI.create(props.endpoint()))
        .region(Region.of(props.region()))
        .credentialsProvider(credentials)
        .serviceConfiguration(
            S3Configuration.builder().pathStyleAccessEnabled(props.pathStyle()).build())
        .build();
  }
}
