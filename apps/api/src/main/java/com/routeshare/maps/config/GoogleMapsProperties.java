package com.routeshare.maps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "routeshare.google-maps")
public record GoogleMapsProperties(boolean enabled, String serverApiKey) {
  public GoogleMapsProperties {
    serverApiKey = serverApiKey == null ? "" : serverApiKey.trim();
  }

  public boolean ready() {
    return enabled && !serverApiKey.isBlank();
  }
}
