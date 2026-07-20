package com.routeshare.maps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "routeshare.google-maps")
public record GoogleMapsProperties(
    boolean enabled,
    String serverApiKey,
    Integer placeDetailsCacheTtlSeconds,
    Integer routeCacheTtlSeconds,
    Integer providerFailureThreshold,
    Integer providerCooldownSeconds) {
  @ConstructorBinding
  public GoogleMapsProperties {
    serverApiKey = serverApiKey == null ? "" : serverApiKey.trim();
    // Google's terms allow caching coordinates/route results for up to 30 days; defaults stay well
    // inside that (1 day for place details, 7 days for distance/directions).
    placeDetailsCacheTtlSeconds = positiveOr(placeDetailsCacheTtlSeconds, 86_400);
    routeCacheTtlSeconds = positiveOr(routeCacheTtlSeconds, 604_800);
    providerFailureThreshold = positiveOr(providerFailureThreshold, 3);
    providerCooldownSeconds = positiveOr(providerCooldownSeconds, 30);
  }

  /** Test/back-compat convenience: only the gating fields, cache/breaker defaults applied. */
  public GoogleMapsProperties(boolean enabled, String serverApiKey) {
    this(enabled, serverApiKey, null, null, null, null);
  }

  public boolean ready() {
    return enabled && !serverApiKey.isBlank();
  }

  private static int positiveOr(Integer value, int fallback) {
    return value == null || value <= 0 ? fallback : value;
  }
}
