package com.routeshare.location.service.impl;

import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.location.dto.response.RealtimeTokenResponse;
import com.routeshare.location.repository.RealtimeChannelRepository;
import com.routeshare.location.service.RealtimeChannelService;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RealtimeChannelServiceImpl implements RealtimeChannelService {
  private final CurrentUserProvider current;
  private final IdentityFacade identity;
  private final RealtimeChannelRepository channels;
  private final MeterRegistry meters;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();
  private final long tokenTtlSeconds;

  public RealtimeChannelServiceImpl(
      CurrentUserProvider current,
      IdentityFacade identity,
      RealtimeChannelRepository channels,
      MeterRegistry meters,
      Clock clock,
      @Value("${routeshare.realtime.token-ttl-seconds:300}") long tokenTtlSeconds) {
    this.current = current;
    this.identity = identity;
    this.channels = channels;
    this.meters = meters;
    this.clock = clock;
    this.tokenTtlSeconds = tokenTtlSeconds;
  }

  @Override
  @Transactional
  public RealtimeTokenResponse issueToken() {
    long appUserId = identity.upsertFromToken(current.requireCurrentUser()).appUserId();
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    Instant expiresAt = Instant.now(clock).plusSeconds(tokenTtlSeconds);
    channels.insertToken(hash(token), appUserId, expiresAt);
    return new RealtimeTokenResponse(
        token,
        expiresAt,
        "/api/v1/realtime/connect?token=" + token,
        "/api/v1/realtime/sse?token=" + token);
  }

  @Override
  @Transactional
  public long consumeToken(String token) {
    Instant now = Instant.now(clock);
    String hash = hash(token);
    long owner =
        channels
            .validTokenOwner(hash, now)
            .orElseThrow(
                () ->
                    new GateConflictException(
                        "REALTIME_TOKEN_EXPIRED",
                        "Realtime token is expired, invalid or already used.",
                        "/api/v1/realtime/token"));
    if (channels.consumeToken(hash, now) != 1) {
      throw new GateConflictException(
          "REALTIME_TOKEN_EXPIRED",
          "Realtime token is expired, invalid or already used.",
          "/api/v1/realtime/token");
    }
    return owner;
  }

  @Override
  @Transactional
  public void connect(long appUserId, String connectionId, String transport) {
    channels.register(appUserId, connectionId, transport, Instant.now(clock));
    meters.counter("routeshare_realtime_connections_total", "transport", transport).increment();
  }

  @Override
  @Transactional
  public void disconnect(String connectionId) {
    channels.disconnect(connectionId);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isConnected(long appUserId) {
    return channels.existsByAppUserId(appUserId);
  }

  @Override
  @Transactional
  public int purgeExpired() {
    Instant now = Instant.now(clock);
    return channels.deleteExpiredChannels(now.minus(Duration.ofHours(24)))
        + channels.deleteExpiredTokens(now)
        + channels.deleteIneligibleChannels();
  }

  private String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }
}
