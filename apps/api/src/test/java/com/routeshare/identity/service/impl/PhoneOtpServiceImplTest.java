package com.routeshare.identity.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.identity.config.NotifyLkProperties;
import com.routeshare.identity.dto.request.OtpRequest;
import com.routeshare.identity.dto.request.OtpVerifyRequest;
import com.routeshare.identity.entity.PhoneOtpChallengeEntity;
import com.routeshare.identity.provider.SmsGateway;
import com.routeshare.identity.repository.PhoneOtpChallengeRepository;
import com.routeshare.identity.service.PhoneOtpAccessTokenService;
import com.routeshare.identity.service.PhoneVerifiedIdentityService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PhoneOtpServiceImplTest {
  private static final String PHONE_E164 = "+94" + "771234567";
  private final PhoneOtpChallengeRepository challenges =
      org.mockito.Mockito.mock(PhoneOtpChallengeRepository.class);
  private final SmsGateway sms = org.mockito.Mockito.mock(SmsGateway.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-06-14T10:00:00Z"), ZoneOffset.UTC);
  private final PhoneOtpServiceImpl.OtpCodeHasher hasher =
      new PhoneOtpServiceImpl.BCryptOtpCodeHasher();
  private final PhoneVerifiedIdentityService identities =
      phone ->
          new PhoneVerifiedIdentityService.VerifiedPhoneUser(
              "kc-user-123", phone, phone, Set.of("PASSENGER"));

  @Test
  void requestsOtpNormalizesPhonePersistsHashAndSendsSms() {
    when(challenges.findFirstByPhoneE164AndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            PHONE_E164, "PENDING", Instant.parse("2026-06-14T10:00:00Z")))
        .thenReturn(Optional.empty());
    when(challenges.save(any(PhoneOtpChallengeEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    var service = new PhoneOtpServiceImpl(challenges, sms, clock, () -> "123456", hasher);

    var response = service.requestOtp(new OtpRequest("077 123 4567"));

    assertThat(response.phoneNumber()).isEqualTo(PHONE_E164);
    assertThat(response.expiresInSeconds()).isEqualTo(300);
    assertThat(response.resendAfterSeconds()).isEqualTo(60);
    verify(challenges).save(any(PhoneOtpChallengeEntity.class));
    verify(sms).sendOtp(PHONE_E164, "123456", 5);
  }

  @Test
  void verifiesCorrectOtpAndMarksChallengeVerified() {
    var id = UUID.randomUUID();
    var challenge =
        PhoneOtpChallengeEntity.pending(
            id,
            PHONE_E164,
            hasher.hash("123456"),
            clock.instant(),
            clock.instant().plusSeconds(300),
            clock.instant().plusSeconds(60));
    when(challenges.findByVerificationIdAndPhoneE164(id, PHONE_E164))
        .thenReturn(Optional.of(challenge));
    var service = otpService();

    var response = service.verifyOtp(new OtpVerifyRequest(id, "0771234567", "123456"));

    assertThat(response.verified()).isTrue();
    assertThat(response.accessToken()).startsWith(PhoneOtpAccessTokenService.TOKEN_PREFIX);
    var jwt =
        new PhoneOtpAccessTokenServiceImpl("routeshare-test-phone-access-token-key-32chars", clock)
            .parse(response.accessToken());
    assertThat(jwt.getSubject()).isEqualTo("kc-user-123");
    assertThat(jwt.getClaimAsString("phone_number")).isEqualTo(PHONE_E164);
    assertThat(response.expiresInSeconds()).isPositive();
    assertThat(challenge.getStatus()).isEqualTo("VERIFIED");
    verify(challenges).save(challenge);
    verify(sms, never()).sendOtp(any(), any(), any(int.class));
  }

  @Test
  void acceptsDemoOtpCodeWhenNotifyLkDemoSenderOtpIsAllowed() {
    var id = UUID.randomUUID();
    var challenge =
        PhoneOtpChallengeEntity.pending(
            id,
            PHONE_E164,
            hasher.hash("123456"),
            clock.instant(),
            clock.instant().plusSeconds(300),
            clock.instant().plusSeconds(60));
    when(challenges.findByVerificationIdAndPhoneE164(id, PHONE_E164))
        .thenReturn(Optional.of(challenge));
    var service =
        new PhoneOtpServiceImpl(
            challenges,
            sms,
            clock,
            () -> "999999",
            hasher,
            new PhoneOtpAccessTokenServiceImpl(
                "routeshare-test-phone-access-token-key-32chars", clock),
            identities,
            notifyLkProperties(true));

    var response = service.verifyOtp(new OtpVerifyRequest(id, "0771234567", "000000"));

    assertThat(response.verified()).isTrue();
    assertThat(challenge.getStatus()).isEqualTo("VERIFIED");
  }

  @Test
  void sendsSmsAndRejectsDemoOtpCodeWhenNotifyLkDemoSenderOtpIsNotAllowed() {
    when(challenges.findFirstByPhoneE164AndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            PHONE_E164, "PENDING", Instant.parse("2026-06-14T10:00:00Z")))
        .thenReturn(Optional.empty());
    when(challenges.save(any(PhoneOtpChallengeEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    var service =
        new PhoneOtpServiceImpl(
            challenges,
            sms,
            clock,
            () -> "123456",
            hasher,
            new PhoneOtpAccessTokenServiceImpl(
                "routeshare-test-phone-access-token-key-32chars", clock),
            identities,
            notifyLkProperties(false));

    var request = service.requestOtp(new OtpRequest("0771234567"));

    verify(sms).sendOtp(PHONE_E164, "123456", 5);

    var challenge =
        PhoneOtpChallengeEntity.pending(
            request.verificationId(),
            PHONE_E164,
            hasher.hash("123456"),
            clock.instant(),
            clock.instant().plusSeconds(300),
            clock.instant().plusSeconds(60));
    when(challenges.findByVerificationIdAndPhoneE164(request.verificationId(), PHONE_E164))
        .thenReturn(Optional.of(challenge));

    assertThatThrownBy(
            () ->
                service.verifyOtp(
                    new OtpVerifyRequest(request.verificationId(), "0771234567", "000000")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Invalid or expired verification code");
  }

  @Test
  void issuedPhoneAccessTokenCanBeParsedAsJwtPrincipal() {
    var tokens = new PhoneOtpAccessTokenServiceImpl("test-phone-token-signing-key-32chars", clock);

    var issued = tokens.issue(PHONE_E164);
    var jwt = tokens.parse(issued.accessToken());

    assertThat(jwt.getSubject()).isEqualTo("phone:" + PHONE_E164);
    assertThat(jwt.getClaimAsString("phone_number")).isEqualTo(PHONE_E164);
    assertThat(jwt.getExpiresAt()).isEqualTo(issued.expiresAt());
  }

  @Test
  void rejectsExpiredOrWrongOtpWithoutRevealingWhichFieldFailed() {
    var id = UUID.randomUUID();
    var challenge =
        PhoneOtpChallengeEntity.pending(
            id,
            PHONE_E164,
            hasher.hash("123456"),
            clock.instant().minusSeconds(600),
            clock.instant().minusSeconds(1),
            clock.instant());
    when(challenges.findByVerificationIdAndPhoneE164(id, PHONE_E164))
        .thenReturn(Optional.of(challenge));
    var service = otpService();

    assertThatThrownBy(() -> service.verifyOtp(new OtpVerifyRequest(id, "0771234567", "000000")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Invalid or expired verification code");
  }

  private PhoneOtpServiceImpl otpService() {
    return new PhoneOtpServiceImpl(
        challenges,
        sms,
        clock,
        () -> "999999",
        hasher,
        new PhoneOtpAccessTokenServiceImpl("routeshare-test-phone-access-token-key-32chars", clock),
        identities,
        notifyLkProperties(false));
  }

  private NotifyLkProperties notifyLkProperties(boolean allowDemoSenderForOtp) {
    return new NotifyLkProperties(
        false,
        java.net.URI.create("https://app.notify.lk/api/v1"),
        "",
        "",
        "",
        allowDemoSenderForOtp,
        "Your RouteShare verification code is %s. It expires in %d minutes.");
  }
}
