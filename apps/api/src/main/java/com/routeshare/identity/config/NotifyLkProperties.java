package com.routeshare.identity.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "routeshare.notify-lk")
public record NotifyLkProperties(
    boolean enabled,
    URI apiBaseUrl,
    String userId,
    String apiKey,
    String senderId,
    boolean allowDemoSenderForOtp,
    String otpMessageTemplate) {
  public NotifyLkProperties {
    apiBaseUrl = apiBaseUrl == null ? URI.create("https://app.notify.lk/api/v1") : apiBaseUrl;
    senderId = senderId == null ? "" : senderId.trim();
    userId = userId == null ? "" : userId.trim();
    apiKey = apiKey == null ? "" : apiKey.trim();
    otpMessageTemplate =
        otpMessageTemplate == null || otpMessageTemplate.isBlank()
            ? "Your RouteShare verification code is %s. It expires in %d minutes."
            : otpMessageTemplate;
  }

  public boolean hasCredentials() {
    return !userId.isBlank() && !apiKey.isBlank() && !senderId.isBlank();
  }
}
