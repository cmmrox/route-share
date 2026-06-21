package com.routeshare.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for passenger trip share-links. {@code baseUrl} is the public web base that renders
 * a share token; {@code defaultTtlMinutes} bounds how long a generated link stays valid.
 */
@ConfigurationProperties(prefix = "routeshare.share")
public record TripShareProperties(
    String baseUrl, Integer defaultTtlMinutes, Integer maxTtlMinutes) {
  public String resolvedBaseUrl() {
    return baseUrl == null || baseUrl.isBlank() ? "https://app.routeshare.lk/share/trip" : baseUrl;
  }

  public int resolvedDefaultTtlMinutes() {
    return defaultTtlMinutes == null || defaultTtlMinutes <= 0 ? 240 : defaultTtlMinutes;
  }

  public int resolvedMaxTtlMinutes() {
    return maxTtlMinutes == null || maxTtlMinutes <= 0 ? 1440 : maxTtlMinutes;
  }
}
