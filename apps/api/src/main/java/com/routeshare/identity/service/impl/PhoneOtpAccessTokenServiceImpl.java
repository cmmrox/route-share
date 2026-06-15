package com.routeshare.identity.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.identity.service.PhoneOtpAccessTokenService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class PhoneOtpAccessTokenServiceImpl implements PhoneOtpAccessTokenService {
  private static final Duration TOKEN_TTL = Duration.ofHours(12);
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final ObjectMapper mapper;
  private final byte[] signingSecret;
  private final Clock clock;

  @Autowired
  public PhoneOtpAccessTokenServiceImpl(
      ObjectMapper mapper,
      @Value(
              "${routeshare.phone-auth.access-token-signing-key:routeshare-local-phone-access-token-key-change-me}")
          String signingKey,
      Clock clock) {
    if (signingKey == null || signingKey.trim().length() < 32) {
      throw new IllegalArgumentException(
          "Phone access token signing key must be at least 32 characters");
    }
    this.mapper = mapper;
    this.signingSecret = signingKey.trim().getBytes(StandardCharsets.UTF_8);
    this.clock = clock;
  }

  public PhoneOtpAccessTokenServiceImpl(String signingKey, Clock clock) {
    this(new ObjectMapper(), signingKey, clock);
  }

  @Override
  public IssuedPhoneOtpToken issue(String phoneNumber) {
    return issue("phone:" + phoneNumber, phoneNumber, phoneNumber);
  }

  @Override
  public IssuedPhoneOtpToken issue(String subject, String phoneNumber, String displayName) {
    Instant issuedAt = clock.instant();
    Instant expiresAt = issuedAt.plus(TOKEN_TTL);
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("iss", "routeshare-phone-auth");
    claims.put("aud", "routeshare-api");
    claims.put("typ", "phone_access");
    claims.put("sub", subject);
    claims.put("phone_number", phoneNumber);
    claims.put("name", displayName);
    claims.put("iat", issuedAt.getEpochSecond());
    claims.put("exp", expiresAt.getEpochSecond());
    claims.put("auth_method", "phone_otp_keycloak_linked");
    String payload = encodeJson(claims);
    String signature = sign(payload);
    return new IssuedPhoneOtpToken(TOKEN_PREFIX + payload + "." + signature, expiresAt);
  }

  @Override
  public Jwt parse(String tokenValue) {
    if (tokenValue == null || !tokenValue.startsWith(TOKEN_PREFIX)) {
      throw new BadJwtException("Unsupported phone token");
    }
    String[] parts = tokenValue.substring(TOKEN_PREFIX.length()).split("\\.", -1);
    if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
      throw new BadJwtException("Malformed phone token");
    }
    String expected = sign(parts[0]);
    if (!constantTimeEquals(expected, parts[1])) {
      throw new BadJwtException("Invalid phone token signature");
    }
    Map<String, Object> claims = decodeClaims(parts[0]);
    Instant issuedAt = instantFromEpoch(claims.get("iat"));
    Instant expiresAt = instantFromEpoch(claims.get("exp"));
    if (expiresAt == null || !expiresAt.isAfter(clock.instant())) {
      throw new BadJwtException("Expired phone token");
    }
    if (!"routeshare-phone-auth".equals(claims.get("iss"))
        || !"routeshare-api".equals(claims.get("aud"))
        || !"phone_access".equals(claims.get("typ"))) {
      throw new BadJwtException("Invalid phone token claims");
    }
    Map<String, Object> headers = Map.of("alg", "HS256", "typ", "JWT", "token_use", "phone");
    return new Jwt(tokenValue, issuedAt, expiresAt, headers, claims);
  }

  private String encodeJson(Map<String, Object> claims) {
    try {
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(mapper.writeValueAsBytes(claims));
    } catch (Exception e) {
      throw new IllegalStateException("Could not create phone token", e);
    }
  }

  private Map<String, Object> decodeClaims(String encoded) {
    try {
      return mapper.readValue(Base64.getUrlDecoder().decode(encoded), MAP_TYPE);
    } catch (Exception e) {
      throw new BadJwtException("Malformed phone token claims", e);
    }
  }

  private String sign(String payload) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(signingSecret, "HmacSHA256"));
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Could not sign phone token", e);
    }
  }

  private boolean constantTimeEquals(String left, String right) {
    byte[] a = left.getBytes(StandardCharsets.UTF_8);
    byte[] b = right.getBytes(StandardCharsets.UTF_8);
    if (a.length != b.length) return false;
    int result = 0;
    for (int i = 0; i < a.length; i++) result |= a[i] ^ b[i];
    return result == 0;
  }

  private Instant instantFromEpoch(Object value) {
    if (value instanceof Number number) return Instant.ofEpochSecond(number.longValue());
    if (value instanceof String string && !string.isBlank())
      return Instant.ofEpochSecond(Long.parseLong(string));
    return null;
  }
}
