package com.routeshare.passenger.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.errors.GateCodes;
import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.errors.GateDeniedException;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.passenger.dto.request.VerificationCaptureUploadRequest;
import com.routeshare.passenger.entity.PassengerProfileEntity;
import com.routeshare.passenger.entity.VerificationSessionEntity;
import com.routeshare.passenger.entity.VerificationStepEntity;
import com.routeshare.passenger.repository.PassengerProfileRepository;
import com.routeshare.passenger.repository.VerificationSessionRepository;
import com.routeshare.passenger.repository.VerificationStepRepository;
import com.routeshare.passenger.service.PassengerDocumentService;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.storage.dto.UploadUrlRequest;
import com.routeshare.storage.dto.UploadUrlResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * POLICY.verifyCameraOnly, at the service boundary.
 *
 * <p>The schema states the same rule with a CHECK that admits only {@code CAMERA}; this is the
 * layer that can tell the client <em>why</em>. Neither is proof — a determined client can lie about
 * where an image came from — which is why the attestation is recorded for the reviewer rather than
 * merely checked and discarded, and why the review step stays human.
 */
class CameraOnlyEnforcementTest {

  private static final long APP_USER = 42L;
  private static final long SESSION = 5L;
  private static final Instant NOW = Instant.parse("2026-08-02T09:41:00Z");

  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identity = mock(IdentityFacade.class);
  private final PassengerProfileRepository profiles = mock(PassengerProfileRepository.class);
  private final VerificationSessionRepository sessions = mock(VerificationSessionRepository.class);
  private final VerificationStepRepository steps = mock(VerificationStepRepository.class);
  private final PassengerDocumentService documents = mock(PassengerDocumentService.class);
  private final PolicySettingService policy = mock(PolicySettingService.class);

  private final PassengerVerificationServiceImpl service =
      new PassengerVerificationServiceImpl(
          current,
          identity,
          profiles,
          sessions,
          steps,
          documents,
          policy,
          mock(NotificationFacade.class),
          new SimpleMeterRegistry(),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @BeforeEach
  void signedInWithAnOpenSession() {
    var token = new CurrentUser("sub", "d@example.test", "+94770000000", "Dinuka", Set.of());
    when(current.requireCurrentUser()).thenReturn(token);
    when(identity.upsertFromToken(token))
        .thenReturn(
            new AppUser(
                APP_USER,
                UUID.randomUUID(),
                "sub",
                "d@example.test",
                "+94770000000",
                "Dinuka",
                "ACTIVE"));
    when(profiles.findEntityByAppUserId(APP_USER)).thenReturn(Optional.of(new PassengerProfile()));
    when(policy.flag(PolicyKey.VERIFY_CAMERA_ONLY)).thenReturn(true);
    when(policy.integer(PolicyKey.VERIFICATION_SESSION_TTL_MINUTES)).thenReturn(30);
    when(sessions.findByIdAndAppUserId(SESSION, APP_USER)).thenReturn(Optional.of(openSession()));
    when(steps.findBySessionIdAndStepKey(SESSION, "NIC_FRONT"))
        .thenReturn(Optional.of(VerificationStepEntity.pending(SESSION, "NIC_FRONT")));
    when(documents.createUploadUrl(any(UploadUrlRequest.class)))
        .thenReturn(new UploadUrlResponse(9L, "key", "https://s3.local/put", "PUT", Map.of(), 300));
  }

  @Test
  @DisplayName("08-10: a gallery capture is refused with CAPTURE_SOURCE_NOT_ALLOWED")
  void galleryCaptureIsRefused() {
    assertThatThrownBy(() -> service.createCaptureUploadUrl("NIC_FRONT", upload("GALLERY")))
        .isInstanceOf(GateDeniedException.class)
        .extracting(ex -> ((GateDeniedException) ex).code())
        .isEqualTo(GateCodes.CAPTURE_SOURCE_NOT_ALLOWED);
  }

  @Test
  @DisplayName("a refused capture never reaches storage — no presigned URL is minted")
  void refusedCaptureNeverGetsAnUploadUrl() {
    assertThatThrownBy(() -> service.createCaptureUploadUrl("NIC_FRONT", upload("SCREENSHOT")))
        .isInstanceOf(GateDeniedException.class);

    verify(documents, never()).createUploadUrl(any());
  }

  @Test
  @DisplayName("a camera capture is accepted and its attestation is stored for the reviewer")
  void cameraCaptureIsAccepted() {
    var response = service.createCaptureUploadUrl("NIC_FRONT", upload("CAMERA"));

    assertThat(response.documentId()).isEqualTo(9L);
    var stored = org.mockito.ArgumentCaptor.forClass(VerificationStepEntity.class);
    verify(steps).save(stored.capture());
    assertThat(stored.getValue().getCaptureSource()).isEqualTo("CAMERA");
    assertThat(stored.getValue().getCapturedAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("the rule is a switch: with VERIFY_CAMERA_ONLY off, a support path can still upload")
  void theRuleIsASwitch() {
    when(policy.flag(PolicyKey.VERIFY_CAMERA_ONLY)).thenReturn(false);

    assertThat(service.createCaptureUploadUrl("NIC_FRONT", upload("GALLERY")).documentId())
        .isEqualTo(9L);
  }

  @Test
  @DisplayName("08-12: a lapsed session refuses the capture")
  void lapsedSessionIsRefused() {
    var lapsed = VerificationSessionEntity.open(APP_USER, NOW.minusSeconds(1));
    lapsed.setId(SESSION);
    when(sessions.findByIdAndAppUserId(SESSION, APP_USER)).thenReturn(Optional.of(lapsed));
    when(sessions.save(any())).thenAnswer(call -> call.getArgument(0));

    assertThatThrownBy(() -> service.createCaptureUploadUrl("NIC_FRONT", upload("CAMERA")))
        .isInstanceOf(GateConflictException.class)
        .extracting(ex -> ((GateConflictException) ex).code())
        .isEqualTo(GateCodes.VERIFICATION_SESSION_EXPIRED);
  }

  @Test
  @DisplayName("08-11: a capture naming somebody else's session is not found")
  void aSessionThatIsNotYoursIsNotFound() {
    when(sessions.findByIdAndAppUserId(SESSION, APP_USER)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.createCaptureUploadUrl("NIC_FRONT", upload("CAMERA")))
        .isInstanceOf(java.util.NoSuchElementException.class);
  }

  @Test
  @DisplayName("08-13: a second attempt while a reviewer holds the first is refused")
  void secondSubmissionWhilePendingIsRefused() {
    var submitted = VerificationSessionEntity.open(APP_USER, NOW.plusSeconds(600));
    submitted.setId(SESSION);
    submitted.markSubmitted(NOW);
    when(sessions.findLive(APP_USER)).thenReturn(Optional.of(submitted));

    assertThatThrownBy(service::startSession)
        .isInstanceOf(GateConflictException.class)
        .extracting(ex -> ((GateConflictException) ex).code())
        .isEqualTo(GateCodes.VERIFICATION_ALREADY_PENDING);
  }

  private static VerificationSessionEntity openSession() {
    var session = VerificationSessionEntity.open(APP_USER, NOW.plusSeconds(600));
    session.setId(SESSION);
    return session;
  }

  private static VerificationCaptureUploadRequest upload(String source) {
    return new VerificationCaptureUploadRequest(
        SESSION, source, NOW, "image/jpeg", 120_000L, "nic-front.jpg");
  }

  /**
   * A profile row with the schema's defaults, which is what a rider who has never saved one has.
   */
  private static class PassengerProfile extends PassengerProfileEntity {}
}
