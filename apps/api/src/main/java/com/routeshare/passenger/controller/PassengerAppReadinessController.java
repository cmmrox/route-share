package com.routeshare.passenger.controller;

import com.routeshare.passenger.dto.request.PassengerDocumentUploadRequest;
import com.routeshare.passenger.dto.response.PassengerVerificationStatusResponse;
import com.routeshare.passenger.service.PassengerDocumentService;
import com.routeshare.storage.dto.UploadUrlRequest;
import com.routeshare.storage.dto.UploadUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PassengerAppReadinessController {
  private final PassengerDocumentService documentService;

  // Ride search is stateless: POST /api/v1/passenger/ride-searches returns scored results directly
  // (real-time matching). The workflow_item GET results/result shells were removed — clients carry
  // the selected result into the ride-detail screen rather than re-fetching a stale persisted set.

  // Early drop-off is served by the real PassengerBookingController (actual-distance fare recalc +
  // capture, Phase 06.6-K); the workflow_item-backed shell was removed here.

  // Trip share / share-link are served by the real PassengerTripShareController +
  // PublicTripShareController (tokenized, time-boxed links + trusted-contact SMS, Phase 06.6-K);
  // the workflow_item-backed shells were removed here.

  // Ratings, SOS, support tickets, notifications, preferences, and push registrations are served by
  // the real rating/safety/support/notification modules (Phase 06.6-D/E); their
  // workflow_item-backed
  // versions were removed here.

  // Payment methods are served by the real tokenized PaymentMethodController (Phase 06.6-C);
  // the workflow_item-backed versions were removed here to avoid duplicate request mappings.

  // Avatar + verification documents now use the real passenger document lifecycle (presigned
  // upload -> submit -> admin review -> signed download), replacing the workflow_item shells.

  @PostMapping("/api/v1/passenger/profile/avatar-upload")
  public UploadUrlResponse avatarUpload(@Valid @RequestBody PassengerDocumentUploadRequest body) {
    return documentService.createUploadUrl(toUploadRequest("AVATAR", body));
  }

  @GetMapping("/api/v1/passenger/verification/status")
  public PassengerVerificationStatusResponse verificationStatus() {
    return documentService.verificationStatus();
  }

  @PostMapping("/api/v1/passenger/verification/documents")
  public UploadUrlResponse verificationDocument(
      @Valid @RequestBody PassengerDocumentUploadRequest body) {
    return documentService.createUploadUrl(toUploadRequest("IDENTITY", body));
  }

  private UploadUrlRequest toUploadRequest(
      String documentType, PassengerDocumentUploadRequest body) {
    return new UploadUrlRequest(
        documentType, body.contentType(), body.fileSizeBytes(), body.originalFilename());
  }
}
