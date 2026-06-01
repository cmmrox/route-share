package com.routeshare.booking.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.booking.dto.request.BookingRequest;
import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.repository.BookingStatusHistoryRepository;
import com.routeshare.booking.service.BookingService;
import com.routeshare.common.repository.IdempotencyKeyRepository;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.pricing.domain.FareCalculator;
import com.routeshare.routing.facade.RoutingFacade;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
  private static final String CONFIRMED = "CONFIRMED";
  private static final String CREATE_BOOKING_OPERATION = "booking:create";
  private static final String INITIAL_CONFIRMATION_REASON =
      "Booking confirmed after occurrence seat reservation";

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final BookingRepository bookings;
  private final BookingStatusHistoryRepository statusHistory;
  private final RoutingFacade routingFacade;
  private final IdempotencyKeyRepository idempotencyKeys;
  private final ObjectMapper objectMapper;
  private final FareCalculator fareCalculator = FareCalculator.defaultSriLankaCalculator();

  @Override
  @Transactional
  public Map<String, Object> book(BookingRequest req, String idempotencyKey) {
    validateIdempotencyKey(idempotencyKey);
    validateMatchedFractions(req);
    CurrentUser user = current.requireCurrentUser();
    String requestHash = requestHash(req);

    var existing =
        idempotencyKeys.findActive(idempotencyKey, user.subject(), CREATE_BOOKING_OPERATION);
    if (existing.isPresent()) {
      return responseFromExisting(existing.get(), requestHash);
    }

    String reservedKey =
        idempotencyKeys.reserveNew(
            idempotencyKey, user.subject(), CREATE_BOOKING_OPERATION, requestHash);
    if (reservedKey == null) {
      return responseFromExisting(
          idempotencyKeys
              .findActive(idempotencyKey, user.subject(), CREATE_BOOKING_OPERATION)
              .orElseThrow(
                  () -> new IllegalStateException("Idempotency request is already in progress")),
          requestHash);
    }

    var app = identityFacade.upsertFromToken(user);
    var reservation =
        routingFacade
            .reserveSeatsAndReturnRouteLength(req.routeOccurrenceId(), req.seats())
            .orElseThrow(
                () -> new IllegalStateException("Insufficient seats or route unavailable"));
    long matchedDistanceMeters =
        Math.round(
            reservation.routeLengthMeters()
                * (req.dropoffRouteFraction() - req.pickupRouteFraction()));
    BigDecimal fareEstimate =
        fareCalculator
            .estimate(matchedDistanceMeters)
            .totalFare()
            .multiply(BigDecimal.valueOf(req.seats()));
    long bookingId = bookings.create(app.appUserId(), req, reservation.routePlanId(), fareEstimate);
    statusHistory.recordInitialStatus(
        bookingId, CONFIRMED, app.appUserId(), INITIAL_CONFIRMATION_REASON);
    Map<String, Object> response =
        Map.of(
            "bookingId",
            bookingId,
            "status",
            CONFIRMED,
            "routeOccurrenceId",
            reservation.routeOccurrenceId(),
            "fareEstimate",
            fareEstimate);
    idempotencyKeys.storeResponse(idempotencyKey, responseBody(response), 200);
    return response;
  }

  private Map<String, Object> responseFromExisting(
      IdempotencyKeyRepository.StoredResponse existing, String requestHash) {
    if (!existing.getRequestHash().equals(requestHash)) {
      throw new IllegalArgumentException(
          "Idempotency-Key was already used with a different request");
    }
    if (!StringUtils.hasText(existing.getResponseBody())) {
      throw new IllegalStateException("Idempotency request is already in progress");
    }
    try {
      return objectMapper.readValue(existing.getResponseBody(), new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Stored idempotency response could not be read", e);
    }
  }

  private void validateIdempotencyKey(String idempotencyKey) {
    if (!StringUtils.hasText(idempotencyKey)) {
      throw new IllegalArgumentException("Idempotency-Key header is required");
    }
    if (idempotencyKey.length() > 200) {
      throw new IllegalArgumentException("Idempotency-Key header is too long");
    }
  }

  private String requestHash(BookingRequest req) {
    try {
      var canonicalJson = objectMapper.writeValueAsString(req);
      var digest = MessageDigest.getInstance("SHA-256");
      return java.util.HexFormat.of()
          .formatHex(digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8)));
    } catch (JsonProcessingException | NoSuchAlgorithmException e) {
      throw new IllegalStateException("Unable to hash booking request", e);
    }
  }

  private String responseBody(Map<String, Object> response) {
    try {
      return objectMapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to store idempotency response", e);
    }
  }

  private void validateMatchedFractions(BookingRequest req) {
    if (req.pickupRouteFraction() >= req.dropoffRouteFraction()) {
      throw new IllegalArgumentException("Pickup must be before drop-off on the matched route");
    }
  }
}
