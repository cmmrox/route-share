package com.routeshare.passenger.service;

import com.routeshare.passenger.dto.request.VerificationCaptureUploadRequest;
import com.routeshare.passenger.dto.request.VerificationDecisionRequest;
import com.routeshare.passenger.dto.request.VerificationStepSubmitRequest;
import com.routeshare.passenger.dto.response.PassengerVerificationResponse;
import com.routeshare.passenger.dto.response.VerificationSessionResponse;
import com.routeshare.storage.dto.UploadUrlResponse;
import java.util.List;

/**
 * P28–P31 — a rider proving who she is.
 *
 * <p>Nothing here may ever refuse a booking. Verification is a badge, a ranking signal and the key
 * to a verified-only trip; a rider at level {@code NONE} books an ordinary trip exactly as she
 * always could, and {@code VerificationNeverBlocksBookingTest} is what stops that quietly changing.
 */
public interface PassengerVerificationService {

  /** P28/P31 — level, per-step status, benefits and, on a rejection, which step and why. */
  PassengerVerificationResponse status();

  /**
   * Opens a capture session with the four steps.
   *
   * @throws com.routeshare.common.errors.GateConflictException {@code VERIFICATION_ALREADY_PENDING}
   *     when a reviewer already has an attempt
   */
  VerificationSessionResponse startSession();

  /**
   * A presigned upload URL for one capture, bound to the session and the step.
   *
   * @throws com.routeshare.common.errors.GateDeniedException {@code CAPTURE_SOURCE_NOT_ALLOWED}
   *     when the capture did not come from the in-app camera
   */
  UploadUrlResponse createCaptureUploadUrl(
      String stepKey, VerificationCaptureUploadRequest request);

  /** Confirms the bytes landed, and moves that one step to SUBMITTED. */
  VerificationSessionResponse submitStep(String stepKey, VerificationStepSubmitRequest request);

  /** All four captured — hands the attempt to a reviewer and moves the rider to PENDING. */
  PassengerVerificationResponse submitForReview();

  /** The reviewer's queue. */
  List<VerificationSessionResponse> pendingForReview();

  /**
   * The human decision. On approval it writes the gender read off the NIC — the only place that
   * value is ever set — and on rejection it names the steps that failed.
   */
  PassengerVerificationResponse decide(long sessionId, VerificationDecisionRequest request);
}
