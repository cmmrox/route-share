package com.routeshare.payment.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cybersource REST credentials. {@code keyId}/{@code sharedSecret} are the HTTP-Signature key pair
 * from the Cybersource portal; {@code webhookSecret} verifies inbound notifications. When {@code
 * enabled} is false the cash-only fallback gateway is used and none of these are required.
 */
@ConfigurationProperties(prefix = "routeshare.cybersource")
public record CybersourceProperties(
    boolean enabled,
    String environment,
    String merchantId,
    String keyId,
    String sharedSecret,
    String captureMode,
    String webhookSecret) {
  public CybersourceProperties {
    environment = environment == null || environment.isBlank() ? "sandbox" : environment;
    captureMode = captureMode == null || captureMode.isBlank() ? "manual" : captureMode;
  }

  /** REST host for the configured environment. */
  public String host() {
    return "production".equalsIgnoreCase(environment)
        ? "api.cybersource.com"
        : "apitest.cybersource.com";
  }

  public boolean ready() {
    return enabled && notBlank(merchantId) && notBlank(keyId) && notBlank(sharedSecret);
  }

  private static boolean notBlank(String s) {
    return s != null && !s.isBlank();
  }
}
