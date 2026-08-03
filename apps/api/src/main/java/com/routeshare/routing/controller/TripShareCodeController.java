package com.routeshare.routing.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.routing.dto.response.TripShareCodeResponse;
import com.routeshare.routing.service.TripShareCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** D14 — sharing a published trip, and what a scanned code resolves to. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TripShareCodeController {

  private final TripShareCodeService shares;

  @GetMapping("/driver/route-occurrences/{routeOccurrenceId}/share")
  @PreAuthorize("hasAnyRole('DRIVER','ADMIN','SUPER_ADMIN')")
  ApiResponse<TripShareCodeResponse> getShare(@PathVariable long routeOccurrenceId) {
    return ApiResponse.ok(shares.getFor(routeOccurrenceId));
  }

  @PostMapping("/driver/route-occurrences/{routeOccurrenceId}/share")
  @PreAuthorize("hasAnyRole('DRIVER','ADMIN','SUPER_ADMIN')")
  ApiResponse<TripShareCodeResponse> share(@PathVariable long routeOccurrenceId) {
    return ApiResponse.ok(shares.shareFor(routeOccurrenceId));
  }

  @DeleteMapping("/driver/route-occurrences/{routeOccurrenceId}/share")
  @PreAuthorize("hasAnyRole('DRIVER','ADMIN','SUPER_ADMIN')")
  ApiResponse<TripShareCodeResponse> revoke(@PathVariable long routeOccurrenceId) {
    return ApiResponse.ok(shares.revoke(routeOccurrenceId));
  }

  /**
   * What a shared link opens. Public because the whole point is that it works for somebody who has
   * never installed the app — and it discloses only the occurrence id, which every search result
   * already carries.
   */
  @GetMapping("/public/trip-links/{shortCode}")
  ApiResponse<TripShareCodeResponse> resolve(@PathVariable String shortCode) {
    long occurrenceId = shares.resolve(shortCode);
    return ApiResponse.ok(new TripShareCodeResponse(occurrenceId, shortCode, null, null, false));
  }

  @GetMapping(value = "/public/trip-links/{shortCode}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
  ResponseEntity<byte[]> qr(@PathVariable String shortCode) {
    // Resolve first: rendering a QR for a revoked or invented code would confirm it was a code.
    shares.resolve(shortCode);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePublic())
        .contentType(MediaType.IMAGE_PNG)
        .body(shares.qrPng(shortCode));
  }
}
