package com.routeshare.passenger.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.passenger.dto.request.PhotoVisibilityRequest;
import com.routeshare.passenger.dto.request.VerificationCaptureUploadRequest;
import com.routeshare.passenger.dto.request.VerificationStepSubmitRequest;
import com.routeshare.passenger.dto.response.PassengerVerificationResponse;
import com.routeshare.passenger.dto.response.PhotoVisibilityResponse;
import com.routeshare.passenger.dto.response.VerificationSessionResponse;
import com.routeshare.passenger.service.PassengerVerificationService;
import com.routeshare.passenger.service.PhotoVisibilityService;
import com.routeshare.storage.dto.UploadUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * P28–P31.
 *
 * <p>{@code isAuthenticated()} rather than a passenger role: in a unified app a driver is also a
 * rider, and verification is a rider-side fact about an account, not a mode.
 */
@RestController
@RequestMapping("/api/v1/passenger")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class PassengerVerificationController {

  private final PassengerVerificationService verification;
  private final PhotoVisibilityService photos;

  /** P28/P31 — level, steps, benefits and any rejection reason. */
  @GetMapping("/verification")
  ApiResponse<PassengerVerificationResponse> status() {
    return ApiResponse.ok(verification.status());
  }

  /** P29 — opens a capture session and returns its four steps. */
  @PostMapping("/verification/sessions")
  ApiResponse<VerificationSessionResponse> startSession() {
    return ApiResponse.ok(verification.startSession());
  }

  @PostMapping("/verification/steps/{stepKey}/upload-url")
  ApiResponse<UploadUrlResponse> uploadUrl(
      @PathVariable String stepKey, @Valid @RequestBody VerificationCaptureUploadRequest request) {
    return ApiResponse.ok(verification.createCaptureUploadUrl(stepKey, request));
  }

  @PostMapping("/verification/steps/{stepKey}/submit")
  ApiResponse<VerificationSessionResponse> submitStep(
      @PathVariable String stepKey, @Valid @RequestBody VerificationStepSubmitRequest request) {
    return ApiResponse.ok(verification.submitStep(stepKey, request));
  }

  /** All four captured — hands the attempt to a reviewer. */
  @PostMapping("/verification/submit")
  ApiResponse<PassengerVerificationResponse> submitForReview() {
    return ApiResponse.ok(verification.submitForReview());
  }

  /** P30. */
  @GetMapping("/profile/photo-visibility")
  ApiResponse<PhotoVisibilityResponse> photoVisibility() {
    return ApiResponse.ok(photos.mine());
  }

  @PutMapping("/profile/photo-visibility")
  ApiResponse<PhotoVisibilityResponse> setPhotoVisibility(
      @Valid @RequestBody PhotoVisibilityRequest request) {
    return ApiResponse.ok(photos.update(request));
  }
}
