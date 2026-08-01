package com.routeshare.common.errors;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final MeterRegistry meters;

  public GlobalExceptionHandler(MeterRegistry meters) {
    this.meters = meters;
  }

  /**
   * A gated refusal renders as data: the app reads {@code code} and {@code actionPath} and shows
   * the matching screen instead of an opaque "access denied".
   *
   * <p>Declared before {@link #denied} matters only to readers — Spring picks the most specific
   * handler — but the pairing is the point: this one is the explained case, that one the residue.
   */
  @ExceptionHandler(GateDeniedException.class)
  ResponseEntity<ApiError> gateDenied(GateDeniedException ex, HttpServletRequest req) {
    meters.counter("routeshare_gate_denied_total", "code", ex.code()).increment();
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiError.gate(ex.code(), ex.getMessage(), ex.actionPath(), correlation(req)));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    var fields =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> new FieldErrorDetail(e.getField(), e.getDefaultMessage()))
            .toList();
    return ResponseEntity.badRequest()
        .body(
            new ApiError(
                "VALIDATION_FAILED",
                "Request validation failed",
                correlation(req),
                null,
                fields,
                java.time.Instant.now()));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiError> constraint(ConstraintViolationException ex, HttpServletRequest req) {
    return ResponseEntity.badRequest()
        .body(ApiError.of("VALIDATION_FAILED", ex.getMessage(), correlation(req)));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
    return ResponseEntity.badRequest()
        .body(
            ApiError.of(
                "BAD_REQUEST",
                "Request body is invalid or contains unsupported enum values",
                correlation(req)));
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ApiError> responseStatus(ResponseStatusException ex, HttpServletRequest req) {
    return ResponseEntity.status(ex.getStatusCode())
        .body(
            ApiError.of(
                "REQUEST_FAILED",
                ex.getReason() == null ? "Request failed" : ex.getReason(),
                correlation(req)));
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiError> denied(AccessDeniedException ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiError.of("ACCESS_DENIED", "Access denied", correlation(req)));
  }

  @ExceptionHandler(GateConflictException.class)
  ResponseEntity<ApiError> gateConflict(GateConflictException ex, HttpServletRequest req) {
    meters.counter("routeshare_gate_denied_total", "code", ex.code()).increment();
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiError.gate(ex.code(), ex.getMessage(), ex.actionPath(), correlation(req)));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiError> badRequest(IllegalArgumentException ex, HttpServletRequest req) {
    return ResponseEntity.badRequest()
        .body(ApiError.of("BAD_REQUEST", ex.getMessage(), correlation(req)));
  }

  @ExceptionHandler(IllegalStateException.class)
  ResponseEntity<ApiError> conflict(IllegalStateException ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiError.of("CONFLICT", ex.getMessage(), correlation(req)));
  }

  @ExceptionHandler({NoSuchElementException.class, EmptyResultDataAccessException.class})
  ResponseEntity<ApiError> notFound(Exception ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiError.of("NOT_FOUND", "Resource not found", correlation(req)));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiError> dataConflict(
      DataIntegrityViolationException ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ApiError.of(
                "DATA_CONFLICT",
                "Request conflicts with existing data or constraints",
                correlation(req)));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> generic(Exception ex, HttpServletRequest req) {
    var correlation = correlation(req);
    log.error(
        "Unhandled API exception correlationId={} path={}", correlation, req.getRequestURI(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError.of("INTERNAL_ERROR", "Unexpected server error", correlation));
  }

  private String correlation(HttpServletRequest req) {
    var existing = req.getHeader("X-Correlation-Id");
    return existing == null || existing.isBlank() ? UUID.randomUUID().toString() : existing;
  }
}
