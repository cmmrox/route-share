package com.routeshare.common.errors;

import java.time.Instant;
import java.util.List;

/**
 * @param actionPath where the app should send the user to clear the refusal. Only gated 403s
 *     populate it; every other error leaves it null.
 */
public record ApiError(
    String code,
    String message,
    String correlationId,
    String actionPath,
    List<FieldErrorDetail> fieldErrors,
    Instant timestamp) {
  public static ApiError of(String code, String message, String correlationId) {
    return new ApiError(code, message, correlationId, null, List.of(), Instant.now());
  }

  public static ApiError gate(
      String code, String message, String actionPath, String correlationId) {
    return new ApiError(code, message, correlationId, actionPath, List.of(), Instant.now());
  }
}
