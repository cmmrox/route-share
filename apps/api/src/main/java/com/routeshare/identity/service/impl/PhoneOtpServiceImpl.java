package com.routeshare.identity.service.impl;

import com.routeshare.identity.config.OtpDevBypassProperties;
import com.routeshare.identity.dto.request.OtpRequest;
import com.routeshare.identity.dto.request.OtpVerifyRequest;
import com.routeshare.identity.dto.response.OtpRequestResponse;
import com.routeshare.identity.dto.response.OtpVerifyResponse;
import com.routeshare.identity.entity.PhoneOtpChallengeEntity;
import com.routeshare.identity.provider.SmsGateway;
import com.routeshare.identity.repository.PhoneOtpChallengeRepository;
import com.routeshare.identity.service.PhoneOtpAccessTokenService;
import com.routeshare.identity.service.PhoneOtpService;
import com.routeshare.identity.service.PhoneVerifiedIdentityService;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PhoneOtpServiceImpl implements PhoneOtpService {
  private static final Duration OTP_TTL = Duration.ofMinutes(5);
  private static final Duration RESEND_DELAY = Duration.ofSeconds(60);
  private static final int MAX_ATTEMPTS = 5;

  private final PhoneOtpChallengeRepository challenges;
  private final SmsGateway smsGateway;
  private final Clock clock;
  private final Supplier<String> codeSupplier;
  private final OtpCodeHasher hasher;
  private final PhoneOtpAccessTokenService accessTokens;
  private final PhoneVerifiedIdentityService phoneIdentities;
  private final OtpDevBypassProperties devBypass;

  @Autowired
  public PhoneOtpServiceImpl(
      PhoneOtpChallengeRepository challenges,
      SmsGateway smsGateway,
      Clock clock,
      PhoneOtpAccessTokenService accessTokens,
      PhoneVerifiedIdentityService phoneIdentities,
      OtpDevBypassProperties devBypass) {
    this(
        challenges,
        smsGateway,
        clock,
        new SecureOtpCodeSupplier(),
        new BCryptOtpCodeHasher(),
        accessTokens,
        phoneIdentities,
        devBypass);
  }

  PhoneOtpServiceImpl(
      PhoneOtpChallengeRepository challenges,
      SmsGateway smsGateway,
      Clock clock,
      Supplier<String> codeSupplier,
      OtpCodeHasher hasher) {
    this(
        challenges,
        smsGateway,
        clock,
        codeSupplier,
        hasher,
        new PhoneOtpAccessTokenServiceImpl("routeshare-test-phone-access-token-key", clock),
        phone ->
            new PhoneVerifiedIdentityService.VerifiedPhoneUser(
                "phone:" + phone, phone, phone, java.util.Set.of("PASSENGER")),
        new OtpDevBypassProperties(false, ""));
  }

  PhoneOtpServiceImpl(
      PhoneOtpChallengeRepository challenges,
      SmsGateway smsGateway,
      Clock clock,
      Supplier<String> codeSupplier,
      OtpCodeHasher hasher,
      PhoneOtpAccessTokenService accessTokens,
      PhoneVerifiedIdentityService phoneIdentities) {
    this(
        challenges,
        smsGateway,
        clock,
        codeSupplier,
        hasher,
        accessTokens,
        phoneIdentities,
        new OtpDevBypassProperties(false, ""));
  }

  PhoneOtpServiceImpl(
      PhoneOtpChallengeRepository challenges,
      SmsGateway smsGateway,
      Clock clock,
      Supplier<String> codeSupplier,
      OtpCodeHasher hasher,
      PhoneOtpAccessTokenService accessTokens,
      PhoneVerifiedIdentityService phoneIdentities,
      OtpDevBypassProperties devBypass) {
    this.challenges = challenges;
    this.smsGateway = smsGateway;
    this.clock = clock;
    this.codeSupplier = codeSupplier;
    this.hasher = hasher;
    this.accessTokens = accessTokens;
    this.phoneIdentities = phoneIdentities;
    this.devBypass = devBypass;
  }

  @Override
  @Transactional
  public OtpRequestResponse requestOtp(OtpRequest request) {
    String phone = normalizeSriLankanMobile(request.phoneNumber());
    Instant now = clock.instant();

    challenges
        .findFirstByPhoneE164AndStatusAndExpiresAtAfterOrderByCreatedAtDesc(phone, "PENDING", now)
        .filter(challenge -> challenge.getResendAvailableAt().isAfter(now))
        .ifPresent(
            challenge -> {
              long waitSeconds =
                  Duration.between(now, challenge.getResendAvailableAt()).toSeconds();
              throw new ResponseStatusException(
                  HttpStatus.TOO_MANY_REQUESTS,
                  "Please wait " + waitSeconds + " seconds before retrying");
            });

    String code = codeSupplier.get();
    UUID verificationId = UUID.randomUUID();
    var challenge =
        PhoneOtpChallengeEntity.pending(
            verificationId,
            phone,
            hasher.hash(code),
            now,
            now.plus(OTP_TTL),
            now.plus(RESEND_DELAY));
    challenges.save(challenge);
    if (!devBypass.isEnabled()) {
      smsGateway.sendOtp(phone, code, (int) OTP_TTL.toMinutes());
    }

    return new OtpRequestResponse(
        verificationId, phone, (int) OTP_TTL.toSeconds(), (int) RESEND_DELAY.toSeconds());
  }

  @Override
  @Transactional
  public OtpVerifyResponse verifyOtp(OtpVerifyRequest request) {
    String phone = normalizeSriLankanMobile(request.phoneNumber());
    Instant now = clock.instant();
    var challenge =
        challenges
            .findByVerificationIdAndPhoneE164(request.verificationId(), phone)
            .orElseThrow(this::invalidOtp);

    if (!"PENDING".equals(challenge.getStatus()) || !challenge.getExpiresAt().isAfter(now)) {
      if ("PENDING".equals(challenge.getStatus())) {
        challenge.markExpired();
        challenges.save(challenge);
      }
      throw invalidOtp();
    }

    boolean codeAccepted =
        devBypass.accepts(request.code()) || hasher.matches(request.code(), challenge.getOtpHash());
    if (challenge.getAttempts() >= MAX_ATTEMPTS || !codeAccepted) {
      challenge.registerFailedAttempt();
      challenges.save(challenge);
      throw invalidOtp();
    }

    var keycloakUser = phoneIdentities.ensurePassengerUser(phone);
    challenge.markVerified(now);
    challenges.save(challenge);
    var accessToken =
        accessTokens.issue(
            keycloakUser.subject(), keycloakUser.phoneNumber(), keycloakUser.displayName());
    long expiresInSeconds = Duration.between(now, accessToken.expiresAt()).toSeconds();
    return new OtpVerifyResponse(true, phone, accessToken.accessToken(), expiresInSeconds);
  }

  static String normalizeSriLankanMobile(String input) {
    String digits = input == null ? "" : input.replaceAll("\\D", "");
    if (digits.startsWith("0")) {
      digits = "94" + digits.substring(1);
    }
    if (!digits.startsWith("94") && digits.length() == 9 && digits.startsWith("7")) {
      digits = "94" + digits;
    }
    if (!digits.matches("947\\d{8}")) {
      throw new IllegalArgumentException("Phone number must be a valid Sri Lankan mobile number");
    }
    return "+" + digits;
  }

  private ResponseStatusException invalidOtp() {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Invalid or expired verification code");
  }

  interface OtpCodeHasher {
    String hash(String code);

    boolean matches(String code, String hash);
  }

  static final class BCryptOtpCodeHasher implements OtpCodeHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public String hash(String code) {
      return encoder.encode(code);
    }

    @Override
    public boolean matches(String code, String hash) {
      return encoder.matches(code, hash);
    }
  }

  private static final class SecureOtpCodeSupplier implements Supplier<String> {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String get() {
      return String.format("%06d", random.nextInt(900_000) + 100_000);
    }
  }
}
