package com.routeshare.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "routeshare.rate-limit")
public record RateLimitProperties(
    Boolean enabled,
    Integer otpRequestPerHour,
    Integer otpVerifyPerHour,
    Integer paymentIntentPerMinute,
    Integer sosPerMinute) {
  public RateLimitProperties {
    enabled = enabled == null || enabled;
    otpRequestPerHour = positiveOr(otpRequestPerHour, 5);
    otpVerifyPerHour = positiveOr(otpVerifyPerHour, 10);
    paymentIntentPerMinute = positiveOr(paymentIntentPerMinute, 10);
    sosPerMinute = positiveOr(sosPerMinute, 5);
  }

  private static int positiveOr(Integer v, int fallback) {
    return v == null || v <= 0 ? fallback : v;
  }
}
