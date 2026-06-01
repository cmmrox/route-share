package com.routeshare.common.errors;

import java.time.Instant;
import java.util.List;

public record ApiError(
    String code,
    String message,
    String correlationId,
    List<FieldErrorDetail> fieldErrors,
    Instant timestamp) {
  public static ApiError of(String code, String message, String correlationId) {
    return new ApiError(code, message, correlationId, List.of(), Instant.now());
  }
}
