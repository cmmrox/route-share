package com.routeshare.identity.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.identity.config.NotifyLkProperties;
import java.io.IOException;
import java.net.URI;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class NotifyLkSmsGateway implements SmsGateway {
  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final NotifyLkProperties properties;

  public NotifyLkSmsGateway(
      RestClient notifyLkRestClient, ObjectMapper objectMapper, NotifyLkProperties properties) {
    this.restClient = notifyLkRestClient;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  @Override
  public void sendOtp(String phoneE164, String code, int expiresInMinutes) {
    if (!properties.enabled()) {
      throw new IllegalStateException("Notify.lk SMS is disabled for this environment");
    }
    if (!properties.hasCredentials()) {
      throw new IllegalStateException("Notify.lk credentials are not configured");
    }
    if (isDemoSender() && !properties.allowDemoSenderForOtp()) {
      throw new IllegalStateException(
          "Configure an approved Notify.lk sender ID before sending OTP messages");
    }

    String response;
    try {
      response =
          restClient
              .get()
              .uri(
                  builder ->
                      builder
                          .scheme(properties.apiBaseUrl().getScheme())
                          .host(properties.apiBaseUrl().getHost())
                          .port(resolvePort(properties.apiBaseUrl()))
                          .path(joinPath(properties.apiBaseUrl().getPath(), "/send"))
                          .queryParam("user_id", properties.userId())
                          .queryParam("api_key", properties.apiKey())
                          .queryParam("sender_id", properties.senderId())
                          .queryParam("to", toNotifyPhone(phoneE164))
                          .queryParam("message", message(code, expiresInMinutes))
                          .build())
              .retrieve()
              .body(String.class);
    } catch (RestClientResponseException e) {
      throw new IllegalStateException("Notify.lk rejected the OTP request: " + providerError(e), e);
    }

    if (!isSuccess(response)) {
      throw new IllegalStateException("Notify.lk failed to send OTP SMS");
    }
  }

  private String providerError(RestClientResponseException e) {
    try {
      JsonNode root = objectMapper.readTree(e.getResponseBodyAsString());
      String errors = root.path("errors").asText();
      if (!errors.isBlank()) {
        return errors;
      }
      String message = root.path("message").asText();
      if (!message.isBlank()) {
        return message;
      }
    } catch (IOException ignored) {
      // Fall through to generic status text.
    }
    return e.getStatusCode().value() + " " + e.getStatusText();
  }

  private boolean isDemoSender() {
    return "NotifyDEMO".equalsIgnoreCase(properties.senderId());
  }

  private String message(String code, int expiresInMinutes) {
    return String.format(properties.otpMessageTemplate(), code, expiresInMinutes);
  }

  private String toNotifyPhone(String phoneE164) {
    if (phoneE164 == null || !phoneE164.matches("\\+947\\d{8}")) {
      throw new IllegalArgumentException("Phone number must be a Sri Lankan mobile E.164 number");
    }
    return phoneE164.substring(1);
  }

  private boolean isSuccess(String response) {
    if (response == null || response.isBlank()) {
      return false;
    }
    try {
      JsonNode root = objectMapper.readTree(response);
      return "success".equalsIgnoreCase(root.path("status").asText());
    } catch (IOException e) {
      throw new IllegalStateException("Notify.lk returned malformed JSON", e);
    }
  }

  private int resolvePort(URI uri) {
    return uri.getPort() == -1 ? -1 : uri.getPort();
  }

  private String joinPath(String basePath, String suffix) {
    String normalizedBase = basePath == null || basePath.isBlank() ? "" : basePath;
    if (normalizedBase.endsWith("/")) {
      normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
    }
    return normalizedBase + suffix;
  }
}
