package com.routeshare.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "routeshare.rate-limit")
public record RateLimitProperties(
    Boolean enabled,
    Integer otpRequestPerHour,
    Integer otpVerifyPerHour,
    Integer paymentIntentPerMinute,
    Integer sosPerMinute,
    Integer placesAutocompletePerMinute,
    Integer placesDetailsPerMinute,
    Integer directionsPerMinute) {
  @ConstructorBinding
  public RateLimitProperties {
    enabled = enabled == null || enabled;
    otpRequestPerHour = positiveOr(otpRequestPerHour, 5);
    otpVerifyPerHour = positiveOr(otpVerifyPerHour, 10);
    paymentIntentPerMinute = positiveOr(paymentIntentPerMinute, 10);
    sosPerMinute = positiveOr(sosPerMinute, 5);
    // Google-billed proxy endpoints: generous for a human, a hard stop for loops/abuse.
    placesAutocompletePerMinute = positiveOr(placesAutocompletePerMinute, 40);
    placesDetailsPerMinute = positiveOr(placesDetailsPerMinute, 20);
    directionsPerMinute = positiveOr(directionsPerMinute, 20);
  }

  /** Back-compat convenience: pre-maps-limit fields only, maps limits defaulted. */
  public RateLimitProperties(
      Boolean enabled,
      Integer otpRequestPerHour,
      Integer otpVerifyPerHour,
      Integer paymentIntentPerMinute,
      Integer sosPerMinute) {
    this(
        enabled,
        otpRequestPerHour,
        otpVerifyPerHour,
        paymentIntentPerMinute,
        sosPerMinute,
        null,
        null,
        null);
  }

  private static int positiveOr(Integer v, int fallback) {
    return v == null || v <= 0 ? fallback : v;
  }
}
