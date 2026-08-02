package com.routeshare.passenger.service.impl;

import com.routeshare.common.errors.GateCodes;
import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.errors.GateDeniedException;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.passenger.domain.Gender;
import com.routeshare.passenger.domain.VerificationLevel;
import com.routeshare.passenger.domain.VerificationStepKey;
import com.routeshare.passenger.dto.request.VerificationCaptureUploadRequest;
import com.routeshare.passenger.dto.request.VerificationDecisionRequest;
import com.routeshare.passenger.dto.request.VerificationStepSubmitRequest;
import com.routeshare.passenger.dto.response.PassengerVerificationResponse;
import com.routeshare.passenger.dto.response.VerificationSessionResponse;
import com.routeshare.passenger.entity.PassengerProfileEntity;
import com.routeshare.passenger.entity.VerificationSessionEntity;
import com.routeshare.passenger.entity.VerificationStepEntity;
import com.routeshare.passenger.repository.PassengerProfileRepository;
import com.routeshare.passenger.repository.VerificationSessionRepository;
import com.routeshare.passenger.repository.VerificationStepRepository;
import com.routeshare.passenger.service.PassengerDocumentService;
import com.routeshare.passenger.service.PassengerVerificationService;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.storage.dto.UploadUrlRequest;
import com.routeshare.storage.dto.UploadUrlResponse;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * P28–P31.
 *
 * <p>Two rules run through all of it. Verification never refuses a booking — every path here writes
 * a level and none of them is ever read as a gate. And the capture attestation is recorded for the
 * reviewer rather than merely checked: a client can lie about where an image came from, so the
 * value of {@code captureSource} is that a human sees what was claimed, not that the server
 * believed it.
 */
@Service
@RequiredArgsConstructor
public class PassengerVerificationServiceImpl implements PassengerVerificationService {

  private static final String DOCUMENT_TYPE_PREFIX = "IDENTITY_";

  private final CurrentUserProvider current;
  private final IdentityFacade identity;
  private final PassengerProfileRepository profiles;
  private final VerificationSessionRepository sessions;
  private final VerificationStepRepository steps;
  private final PassengerDocumentService documents;
  private final PolicySettingService policy;
  private final NotificationFacade notifications;
  private final MeterRegistry meters;
  private final Clock clock;

  @Override
  @Transactional
  public PassengerVerificationResponse status() {
    long appUserId = currentAppUserId();
    var profile = profile(appUserId);
    var session = sessions.findLatest(appUserId).map(this::expireIfLapsed);
    return new PassengerVerificationResponse(
        profile.getVerificationLevel(),
        profile.getVerifiedAt(),
        false,
        session.map(VerificationSessionEntity::getId).orElse(null),
        session.map(s -> stepViews(s.getId())).orElseGet(this::emptyStepViews),
        PassengerVerificationResponse.benefits(policy.integer(PolicyKey.VERIFIED_RIDES_SHARE_PCT)),
        session.map(VerificationSessionEntity::getDecisionNote).orElse(null));
  }

  @Override
  @Transactional
  public VerificationSessionResponse startSession() {
    long appUserId = currentAppUserId();
    profile(appUserId);

    var live = sessions.findLive(appUserId).map(this::expireIfLapsed);
    // A reviewer already holding the four captures must not have a fifth arrive underneath him.
    if (live.filter(s -> VerificationSessionEntity.SUBMITTED.equals(s.getStatus())).isPresent()) {
      throw new GateConflictException(
          GateCodes.VERIFICATION_ALREADY_PENDING,
          "Your verification is already with our team. We'll let you know as soon as it's checked.",
          "/passenger/verification");
    }
    // An OPEN session that has not lapsed is resumed rather than replaced: a rider who backgrounded
    // the app after two captures should not have to take them again.
    var open = live.filter(s -> VerificationSessionEntity.OPEN.equals(s.getStatus()));
    if (open.isPresent()) {
      return toSessionResponse(open.get());
    }

    var created =
        sessions.save(
            VerificationSessionEntity.open(
                appUserId,
                clock
                    .instant()
                    .plus(
                        Duration.ofMinutes(
                            policy.integer(PolicyKey.VERIFICATION_SESSION_TTL_MINUTES)))));
    VerificationStepKey.inOrder()
        .forEach(key -> steps.save(VerificationStepEntity.pending(created.getId(), key.name())));
    return toSessionResponse(created);
  }

  @Override
  @Transactional
  public UploadUrlResponse createCaptureUploadUrl(
      String stepKey, VerificationCaptureUploadRequest request) {
    VerificationStepKey key = VerificationStepKey.of(stepKey);
    var session = requireOpenSession(request.sessionId());
    var step = requireStep(session.getId(), key);

    requireCameraCapture(request.captureSource());

    var upload =
        documents.createUploadUrl(
            new UploadUrlRequest(
                DOCUMENT_TYPE_PREFIX + key.name(),
                request.contentType(),
                request.fileSizeBytes(),
                request.originalFilename()));
    step.attachCapture(upload.documentId(), VerificationStepEntity.CAMERA, request.capturedAt());
    steps.save(step);
    return upload;
  }

  @Override
  @Transactional
  public VerificationSessionResponse submitStep(
      String stepKey, VerificationStepSubmitRequest request) {
    VerificationStepKey key = VerificationStepKey.of(stepKey);
    var session = requireOpenSessionForCurrentUser();
    var step = requireStep(session.getId(), key);
    if (step.getDocumentId() == null || !step.getDocumentId().equals(request.documentId())) {
      throw new IllegalArgumentException(
          "That capture does not belong to this step. Request an upload URL for it first.");
    }
    // Confirms the bytes actually reached storage; a step marked submitted on a missing object is a
    // reviewer opening a broken image and rejecting a rider who did nothing wrong.
    documents.submit(step.getDocumentId());
    step.markSubmitted();
    steps.save(step);
    return toSessionResponse(session);
  }

  @Override
  @Transactional
  public PassengerVerificationResponse submitForReview() {
    long appUserId = currentAppUserId();
    var session = requireOpenSessionForCurrentUser();
    List<VerificationStepEntity> all = steps.findBySessionIdOrderByIdAsc(session.getId());
    List<String> missing =
        all.stream()
            .filter(s -> !VerificationStepEntity.SUBMITTED.equals(s.getStatus()))
            .map(VerificationStepEntity::getStepKey)
            .toList();
    if (!missing.isEmpty()) {
      throw new IllegalArgumentException("These captures are still outstanding: " + missing);
    }

    session.markSubmitted(clock.instant());
    sessions.save(session);
    setLevel(appUserId, VerificationLevel.PENDING, null);
    meters.counter("routeshare_verification_submissions_total").increment();
    return status();
  }

  @Override
  @Transactional(readOnly = true)
  public List<VerificationSessionResponse> pendingForReview() {
    return sessions.findByStatusOrderByIdAsc(VerificationSessionEntity.SUBMITTED).stream()
        .map(this::toSessionResponse)
        .toList();
  }

  @Override
  @Transactional
  public PassengerVerificationResponse decide(long sessionId, VerificationDecisionRequest request) {
    long reviewerAppUserId = currentAppUserId();
    var session =
        sessions
            .findById(sessionId)
            .orElseThrow(() -> new java.util.NoSuchElementException("Verification not found"));
    if (!VerificationSessionEntity.SUBMITTED.equals(session.getStatus())) {
      throw new IllegalStateException("This verification is not waiting for a decision");
    }

    boolean approved = VerificationSessionEntity.APPROVED.equals(request.decision());
    Set<String> rejected =
        new LinkedHashSet<>(request.rejectedSteps() == null ? List.of() : request.rejectedSteps());
    List<VerificationStepEntity> all = steps.findBySessionIdOrderByIdAsc(session.getId());
    // P31c names the step that failed. "Try again" over four images is a rider who will get it
    // wrong a second time, and a second review nobody needed.
    all.forEach(
        step -> {
          if (approved && !rejected.contains(step.getStepKey())) {
            step.approve();
          } else if (rejected.contains(step.getStepKey())) {
            step.reject(request.note());
          } else {
            step.approve();
          }
        });
    steps.saveAll(all);

    session.decide(
        approved ? VerificationSessionEntity.APPROVED : VerificationSessionEntity.REJECTED,
        reviewerAppUserId,
        request.note(),
        clock.instant());
    sessions.save(session);

    long riderAppUserId = session.getAppUserId();
    if (approved) {
      // The one place gender is ever written. Read off the NIC by the reviewer, never declared by
      // the rider, and never emitted by any endpoint.
      setLevel(riderAppUserId, VerificationLevel.VERIFIED, Gender.of(request.gender()));
    } else {
      setLevel(riderAppUserId, VerificationLevel.REJECTED, null);
    }
    meters
        .counter(
            "routeshare_verification_decisions_total",
            "outcome",
            approved ? "approved" : "rejected")
        .increment();
    notifications.notifyUser(
        riderAppUserId,
        approved ? "VERIFICATION_APPROVED" : "VERIFICATION_REJECTED",
        approved ? "You're verified" : "Verification needs another try",
        approved
            ? "Your identity is confirmed. Verified-only trips are now open to you."
            : "One of your captures wasn't clear enough. Open verification to see which and retake"
                + " it.",
        Map.of("sessionId", String.valueOf(session.getId())));
    return statusOf(riderAppUserId, session);
  }

  // ── internals ──────────────────────────────────────────────────────────────────────────────────

  /**
   * POLICY.verifyCameraOnly. The schema admits only {@code CAMERA}; this is the same rule stated
   * where the client can be told why, and it is a switch rather than a constant because a
   * support-assisted path may be needed for a rider whose camera will not work.
   */
  private void requireCameraCapture(String captureSource) {
    if (!policy.flag(PolicyKey.VERIFY_CAMERA_ONLY)) {
      return;
    }
    if (!VerificationStepEntity.CAMERA.equalsIgnoreCase(captureSource)) {
      throw new GateDeniedException(
          GateCodes.CAPTURE_SOURCE_NOT_ALLOWED,
          "Take this photo with the in-app camera. A picture from your gallery can't be accepted.",
          "/passenger/verification");
    }
  }

  private VerificationSessionEntity requireOpenSessionForCurrentUser() {
    long appUserId = currentAppUserId();
    var session =
        sessions
            .findLive(appUserId)
            .map(this::expireIfLapsed)
            .filter(s -> VerificationSessionEntity.OPEN.equals(s.getStatus()))
            .orElseThrow(
                () ->
                    new GateConflictException(
                        GateCodes.VERIFICATION_SESSION_EXPIRED,
                        "That verification session has ended. Start again and retake the photos.",
                        "/passenger/verification"));
    return session;
  }

  private VerificationSessionEntity requireOpenSession(long sessionId) {
    long appUserId = currentAppUserId();
    var session =
        sessions
            .findByIdAndAppUserId(sessionId, appUserId)
            .map(this::expireIfLapsed)
            .orElseThrow(
                () -> new java.util.NoSuchElementException("Verification session not found"));
    if (!VerificationSessionEntity.OPEN.equals(session.getStatus())) {
      throw new GateConflictException(
          GateCodes.VERIFICATION_SESSION_EXPIRED,
          "That verification session has ended. Start again and retake the photos.",
          "/passenger/verification");
    }
    return session;
  }

  /**
   * Expiry is decided by the clock on every read rather than by a sweeper, so a session cannot be
   * accepted hours after it lapsed just because no job happened to run.
   */
  private VerificationSessionEntity expireIfLapsed(VerificationSessionEntity session) {
    if (session.hasLapsed(clock.instant())) {
      session.setStatus(VerificationSessionEntity.EXPIRED);
      return sessions.save(session);
    }
    return session;
  }

  private VerificationStepEntity requireStep(long sessionId, VerificationStepKey key) {
    return steps
        .findBySessionIdAndStepKey(sessionId, key.name())
        .orElseThrow(() -> new java.util.NoSuchElementException("Verification step not found"));
  }

  private void setLevel(long appUserId, VerificationLevel level, Gender gender) {
    var profile = profile(appUserId);
    profile.setVerificationLevel(level.name());
    profile.setVerifiedAt(level.isVerified() ? clock.instant() : null);
    if (gender != null) {
      profile.setGender(gender.name());
    }
    profiles.save(profile);
  }

  private PassengerProfileEntity profile(long appUserId) {
    profiles.ensureExists(appUserId);
    return profiles
        .findEntityByAppUserId(appUserId)
        .orElseThrow(() -> new IllegalStateException("Passenger profile could not be created"));
  }

  private PassengerVerificationResponse statusOf(
      long riderAppUserId, VerificationSessionEntity session) {
    var profile = profile(riderAppUserId);
    return new PassengerVerificationResponse(
        profile.getVerificationLevel(),
        profile.getVerifiedAt(),
        false,
        session.getId(),
        stepViews(session.getId()),
        PassengerVerificationResponse.benefits(policy.integer(PolicyKey.VERIFIED_RIDES_SHARE_PCT)),
        session.getDecisionNote());
  }

  private VerificationSessionResponse toSessionResponse(VerificationSessionEntity session) {
    long remaining =
        Math.max(0, Duration.between(clock.instant(), session.getExpiresAt()).toSeconds());
    return new VerificationSessionResponse(
        session.getId(),
        session.getStatus(),
        session.getExpiresAt(),
        remaining,
        policy.flag(PolicyKey.VERIFY_CAMERA_ONLY),
        stepViews(session.getId()));
  }

  private List<VerificationSessionResponse.Step> stepViews(long sessionId) {
    Map<String, VerificationStepEntity> byKey =
        steps.findBySessionIdOrderByIdAsc(sessionId).stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    VerificationStepEntity::getStepKey, s -> s, (a, b) -> a));
    return VerificationStepKey.inOrder().stream()
        .map(
            key -> {
              var step = byKey.get(key.name());
              return new VerificationSessionResponse.Step(
                  key.name(),
                  key.label(),
                  key.hint(),
                  key.guideShape(),
                  step == null ? VerificationStepEntity.PENDING : step.getStatus(),
                  step == null ? null : step.getRejectionReason());
            })
        .toList();
  }

  private List<VerificationSessionResponse.Step> emptyStepViews() {
    return VerificationStepKey.inOrder().stream()
        .map(
            key ->
                new VerificationSessionResponse.Step(
                    key.name(),
                    key.label(),
                    key.hint(),
                    key.guideShape(),
                    VerificationStepEntity.PENDING,
                    null))
        .toList();
  }

  private long currentAppUserId() {
    return identity.upsertFromToken(current.requireCurrentUser()).appUserId();
  }
}
