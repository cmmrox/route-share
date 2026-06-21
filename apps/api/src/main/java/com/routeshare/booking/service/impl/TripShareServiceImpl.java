package com.routeshare.booking.service.impl;

import com.routeshare.booking.config.TripShareProperties;
import com.routeshare.booking.dto.request.ShareTripRequest;
import com.routeshare.booking.dto.response.PublicTripStatusResponse;
import com.routeshare.booking.dto.response.ShareTripResponse;
import com.routeshare.booking.entity.TripShareEntity;
import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.repository.TripShareRepository;
import com.routeshare.booking.service.TripShareService;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.identity.provider.SmsGateway;
import com.routeshare.passenger.facade.PassengerFacade;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripShareServiceImpl implements TripShareService {
  private static final Logger log = LoggerFactory.getLogger(TripShareServiceImpl.class);
  private static final SecureRandom RANDOM = new SecureRandom();

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final BookingRepository bookings;
  private final TripShareRepository tripShares;
  private final PassengerFacade passengerFacade;
  private final SmsGateway smsGateway;
  private final TripShareProperties properties;

  @Override
  @Transactional
  public ShareTripResponse share(long bookingId, ShareTripRequest req) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    bookings
        .findFareEstimateByIdAndPassengerAppUserId(bookingId, app.appUserId())
        .orElseThrow(() -> new AccessDeniedException("Booking does not belong to current user"));

    int ttl = resolveTtlMinutes(req);
    Instant expiresAt = Instant.now().plus(Duration.ofMinutes(ttl));
    String token = newToken();
    tripShares.save(TripShareEntity.create(bookingId, app.appUserId(), token, expiresAt));

    String shareUrl = properties.resolvedBaseUrl() + "/" + token;
    int notified = 0;
    if (req != null && Boolean.TRUE.equals(req.notifyContacts())) {
      notified = notifyTrustedContacts(app.appUserId(), shareUrl);
    }
    return new ShareTripResponse(token, shareUrl, expiresAt, notified);
  }

  @Override
  @Transactional
  public void revoke(long bookingId, String token) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    int updated = tripShares.revoke(token, app.appUserId());
    if (updated != 1) {
      throw new AccessDeniedException("Share link does not belong to current user");
    }
  }

  @Override
  @Transactional(readOnly = true)
  public PublicTripStatusResponse publicStatus(String token) {
    return tripShares
        .findPublicStatusByToken(token, Instant.now())
        .map(
            row ->
                new PublicTripStatusResponse(
                    row.getOriginLabel(),
                    row.getDestinationLabel(),
                    row.getDepartureTime(),
                    row.getBookingStatus(),
                    row.getTripStatus(),
                    row.getPassengerTripStatus(),
                    row.getDriverName(),
                    row.getVehiclePlate(),
                    row.getExpiresAt()))
        .orElseThrow(() -> new NoSuchElementException("Share link is invalid or has expired"));
  }

  private int notifyTrustedContacts(long appUserId, String shareUrl) {
    int notified = 0;
    String message = "Follow my RouteShare trip live: " + shareUrl;
    for (var contact : passengerFacade.findTrustedContacts(appUserId)) {
      if (contact.phone() == null || contact.phone().isBlank()) {
        continue;
      }
      try {
        smsGateway.sendText(contact.phone(), message);
        notified++;
      } catch (RuntimeException e) {
        // Best-effort: link creation must not fail if a single SMS cannot be delivered.
        log.warn("trip_share_sms_failed contact_phone_suffix={}", phoneSuffix(contact.phone()), e);
      }
    }
    return notified;
  }

  private int resolveTtlMinutes(ShareTripRequest req) {
    int requested =
        req == null || req.expiresInMinutes() == null
            ? properties.resolvedDefaultTtlMinutes()
            : req.expiresInMinutes();
    return Math.min(requested, properties.resolvedMaxTtlMinutes());
  }

  private String newToken() {
    byte[] bytes = new byte[24];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String phoneSuffix(String phone) {
    return phone.length() <= 3 ? "***" : phone.substring(phone.length() - 3);
  }
}
