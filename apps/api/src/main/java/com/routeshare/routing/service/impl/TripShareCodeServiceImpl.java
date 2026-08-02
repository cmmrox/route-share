package com.routeshare.routing.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.routeshare.routing.dto.response.TripShareCodeResponse;
import com.routeshare.routing.entity.RouteOccurrenceShareEntity;
import com.routeshare.routing.repository.RouteOccurrenceShareRepository;
import com.routeshare.routing.service.TripShareCodeService;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripShareCodeServiceImpl implements TripShareCodeService {

  /** Crockford-style base32 without I, L, O or U: unambiguous when read off a phone screen. */
  private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

  private static final int CODE_LENGTH = 10;
  private static final int QR_PIXELS = 512;

  private final RouteOccurrenceShareRepository shares;
  private final Clock clock;
  private final String shortLinkBaseUrl;
  private final SecureRandom random = new SecureRandom();

  /**
   * Rendering a QR is pure CPU over a ten-character string, and the same driver's screen asks for
   * it on every refresh. Cached in-process rather than in Redis: the bytes are tiny, identical on
   * every instance, and a cache miss costs a millisecond.
   */
  private final com.github.benmanes.caffeine.cache.Cache<String, byte[]> qrCache =
      com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
          .maximumSize(1_000)
          .expireAfterWrite(1, TimeUnit.HOURS)
          .build();

  public TripShareCodeServiceImpl(
      RouteOccurrenceShareRepository shares,
      Clock clock,
      @Value("${routeshare.short-link.base-url:https://comigo.lk/r/}") String shortLinkBaseUrl) {
    this.shares = shares;
    this.clock = clock;
    this.shortLinkBaseUrl =
        shortLinkBaseUrl.endsWith("/") ? shortLinkBaseUrl : shortLinkBaseUrl + "/";
  }

  @Override
  @Transactional
  public TripShareCodeResponse shareFor(long routeOccurrenceId) {
    // Idempotent on purpose: a driver who taps share twice must not invalidate the link already
    // sitting in somebody's chat.
    var existing = shares.findByRouteOccurrenceId(routeOccurrenceId);
    if (existing.isPresent()) {
      var share = existing.get();
      if (!share.isLive()) {
        // Re-sharing a revoked trip mints a new code rather than reviving the old one, so a link
        // the driver deliberately killed stays dead.
        share.setShortCode(newCode());
        share.revoke(null);
        shares.save(share);
      }
      return toResponse(share);
    }
    return toResponse(shares.save(RouteOccurrenceShareEntity.of(routeOccurrenceId, newCode())));
  }

  @Override
  @Transactional
  public TripShareCodeResponse revoke(long routeOccurrenceId) {
    var share =
        shares
            .findByRouteOccurrenceId(routeOccurrenceId)
            .orElseThrow(() -> new NoSuchElementException("This trip has not been shared"));
    share.revoke(clock.instant());
    return toResponse(shares.save(share));
  }

  @Override
  @Transactional(readOnly = true)
  public long resolve(String shortCode) {
    return shares
        .findByShortCode(
            shortCode == null ? "" : shortCode.trim().toUpperCase(java.util.Locale.ROOT))
        .filter(RouteOccurrenceShareEntity::isLive)
        .map(RouteOccurrenceShareEntity::getRouteOccurrenceId)
        // Deliberately the same answer for "never existed" and "revoked". A 410 would tell a
        // scanner it had found a real code, which is the whole of what it is trying to learn.
        .orElseThrow(() -> new NoSuchElementException("Link not found"));
  }

  @Override
  public byte[] qrPng(String shortCode) {
    return qrCache.get(shortCode, this::renderQr);
  }

  private byte[] renderQr(String shortCode) {
    try {
      var matrix =
          new QRCodeWriter()
              .encode(
                  shortLinkBaseUrl + shortCode,
                  BarcodeFormat.QR_CODE,
                  QR_PIXELS,
                  QR_PIXELS,
                  Map.of(
                      // A code is scanned off a cracked phone screen in daylight; the extra
                      // redundancy costs a few bytes and buys a scan that works first time.
                      EncodeHintType.ERROR_CORRECTION,
                      ErrorCorrectionLevel.M,
                      EncodeHintType.MARGIN,
                      1));
      var out = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", out);
      return out.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("Unable to render the trip QR code", e);
    }
  }

  private String newCode() {
    var code = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      code.append(ALPHABET[random.nextInt(ALPHABET.length)]);
    }
    return code.toString();
  }

  private TripShareCodeResponse toResponse(RouteOccurrenceShareEntity share) {
    return new TripShareCodeResponse(
        share.getRouteOccurrenceId(),
        share.getShortCode(),
        shortLinkBaseUrl + share.getShortCode(),
        "/api/v1/public/trip-links/" + share.getShortCode() + "/qr.png",
        !share.isLive());
  }
}
