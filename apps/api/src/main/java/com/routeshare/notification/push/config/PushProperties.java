package com.routeshare.notification.push.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Firebase Cloud Messaging configuration. {@code serviceAccountPath} points to the downloaded
 * Firebase service-account JSON. When {@code enabled} is false, the logging push adapter is used.
 */
@ConfigurationProperties(prefix = "routeshare.push")
public record PushProperties(boolean enabled, String projectId, String serviceAccountPath) {
  public boolean ready() {
    return enabled && serviceAccountPath != null && !serviceAccountPath.isBlank();
  }
}
