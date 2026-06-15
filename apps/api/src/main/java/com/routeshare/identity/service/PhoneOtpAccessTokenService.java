package com.routeshare.identity.service;

import java.time.Instant;
import org.springframework.security.oauth2.jwt.Jwt;

public interface PhoneOtpAccessTokenService {
  String TOKEN_PREFIX = "rs-phone.";

  IssuedPhoneOtpToken issue(String phoneNumber);

  IssuedPhoneOtpToken issue(String subject, String phoneNumber, String displayName);

  Jwt parse(String tokenValue);

  record IssuedPhoneOtpToken(String accessToken, Instant expiresAt) {}
}
