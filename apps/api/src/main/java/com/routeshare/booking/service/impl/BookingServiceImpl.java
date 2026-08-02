package com.routeshare.booking.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.booking.dto.request.BookingRequest;
import com.routeshare.booking.dto.request.BookingStatusTransitionRequest;
import com.routeshare.booking.dto.response.DriverBookingRequestResponse;
import com.routeshare.booking.dto.response.PassengerBookingDetailResponse;
import com.routeshare.booking.dto.response.PassengerBookingSummaryResponse;
import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.repository.BookingStatusHistoryRepository;
import com.routeshare.booking.service.BookingService;
import com.routeshare.common.repository.IdempotencyKeyRepository;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.pricing.facade.PricingFacade;
import com.routeshare.routing.facade.RoutingFacade;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
  private static final String CONFIRMED = "CONFIRMED";
  private static final String CANCELLED = "CANCELLED";
  private static final String COMPLETED = "COMPLETED";
  private static final String REJECTED = "REJECTED";
  private static final String CREATE_BOOKING_OPERATION = "booking:create";
  private static final String INITIAL_CONFIRMATION_REASON =
      "Booking confirmed after occurrence seat reservation";

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final BookingRepository bookings;
  private final BookingStatusHistoryRepository statusHistory;
  private final RoutingFacade routingFacade;
  private final IdempotencyKeyRepository idempotencyKeys;
  private final NotificationFacade notifications;
  private final ObjectMapper objectMapper;
  private final PricingFacade pricing;
  private final com.routeshare.payment.facade.PaymentFacade payments;
  private final com.routeshare.trip.facade.TripLifecycleFacade tripLifecycle;
  private final com.routeshare.penalty.facade.PenaltyFacade penalties;
  private final com.routeshare.booking.service.SeatHoldService seatHolds;
  private final com.routeshare.platform.service.PolicySettingService policy;
  private final java.time.Clock clock;
  private static final Map<String, Set<String>> ALLOWED_TRANSITIONS =
      Map.of(
          "REQUESTED",
          Set.of(CONFIRMED, REJECTED, CANCELLED),
          CONFIRMED,
          Set.of(CANCELLED, COMPLETED),
          CANCELLED,
          Set.of(),
          REJECTED,
          Set.of(),
          COMPLETED,
          Set.of());

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
    // P11: two unanswered requests at once. The third is refused rather than queued — a rider
    // holding five seats across five cars has taken inventory nobody else can book while deciding.
    int openRequests = bookings.countOpenRequests(app.appUserId());
    if (openRequests
        >= policy.integer(com.routeshare.platform.domain.PolicyKey.MAX_OPEN_PASSENGER_REQUESTS)) {
      throw new com.routeshare.common.errors.GateConflictException(
          "TOO_MANY_OPEN_REQUESTS",
          "You already have "
              + openRequests
              + " requests waiting for a driver. Cancel one, or wait"
              + " for a reply.",
          "/passenger/bookings");
    }
    var reservation =
        routingFacade
            .reserveSeatsAndReturnRouteLength(req.routeOccurrenceId(), req.seats())
            .orElseThrow(
                () -> new IllegalStateException("Insufficient seats or route unavailable"));
    long matchedDistanceMeters =
        Math.round(
            reservation.routeLengthMeters()
                * (req.dropoffRouteFraction() - req.pickupRouteFraction()));
    // Overlap percent is what the rider actually shares of the driver's road, which is what the
    // discount tier is banded on.
    BigDecimal matchPercent =
        BigDecimal.valueOf((req.dropoffRouteFraction() - req.pickupRouteFraction()) * 100)
            .setScale(2, RoundingMode.HALF_UP);
    var quote =
        pricing.quoteForMatch(
            reservation.routeOccurrenceId(),
            reservation.vehicleId(),
            BigDecimal.valueOf(matchedDistanceMeters),
            matchPercent,
            req.seats());
    // fare_estimate mirrors passengerPays so existing readers keep working; the quote is the
    // record of what was charged and why.
    BigDecimal fareEstimate = quote.passengerPays();
    long bookingId = bookings.create(app.appUserId(), req, reservation.routePlanId(), fareEstimate);
    // Stored rather than passed straight through, because an approve-each booking authorises when
    // the driver accepts — by which time the request that named the card is long gone.
    bookings.recordChosenPaymentMethod(bookingId, req.paymentMethodId());
    pricing.persistForBooking(
        bookingId,
        reservation.routeOccurrenceId(),
        reservation.vehicleId(),
        app.appUserId(),
        BigDecimal.valueOf(matchedDistanceMeters),
        matchPercent,
        req.seats());
    // The named slots are held in the same transaction as the booking row, so the race between two
    // riders taking the last seat is decided by the unique index rather than by whichever request
    // happened to read the counter first.
    var heldSeats =
        seatHolds.hold(bookingId, reservation.routeOccurrenceId(), req.seatSlotIds(), req.seats());

    // D13: instant-book confirms now; approve-each waits for the driver and lapses if he never
    // answers. The mode belongs to the occurrence, so the client never gets to choose it.
    var approvalMode = seatHolds.approvalModeFor(reservation.routeOccurrenceId());
    boolean confirmed = approvalMode.confirmsImmediately();
    java.time.Instant expiresAt =
        confirmed
            ? null
            : clock
                .instant()
                .plus(
                    java.time.Duration.ofMinutes(
                        policy.integer(
                            com.routeshare.platform.domain.PolicyKey
                                .SCHEDULED_REQUEST_EXPIRY_MINUTES)));
    String initialStatus = confirmed ? CONFIRMED : "REQUESTED";
    bookings.applyApprovalOutcome(bookingId, initialStatus, expiresAt);
    statusHistory.recordInitialStatus(
        bookingId,
        initialStatus,
        app.appUserId(),
        confirmed ? INITIAL_CONFIRMATION_REASON : "Request sent to the driver for approval");
    // Only a confirmed seat earns a trip and a card hold. A request the driver has not answered is
    // not somebody riding: materialising a trip for it would put an unanswered request under the
    // start-buffer sweeper, and authorising for it would hold money on a seat that may never exist.
    var appliedDues = com.routeshare.penalty.dto.response.AppliedDuesResponse.empty();
    if (confirmed) {
      // The occurrence becomes a trip the moment somebody is actually riding on it, and the
      // start-buffer clock opens with it.
      tripLifecycle.ensureTripForBookedOccurrence(reservation.routeOccurrenceId());
      // Her own clock, distinct from the trip's (P35). It decides whether a cancel is free, so the
      // promised time is derived server-side and never taken from the request.
      tripLifecycle.openLateGraceForBooking(bookingId);
      // The card is held now and charged when the driver starts. Accepting does not charge;
      // approval does not charge; a trip that never starts costs the passenger nothing.
      payments.authorizeForBooking(bookingId, req.paymentMethodId(), fareEstimate);
      // Fees she could not be charged for at the time ride along to this checkout (P09d). They are
      // added to the total, never a gate on making the booking: refusing a rider over an unpaid
      // LKR 49 turns a small fee into a lost passenger, and P25 shows dues as a line, not a wall.
      appliedDues = penalties.applyOutstandingDues(app.appUserId(), bookingId);
      if (appliedDues.total().signum() > 0) {
        bookings.recordAppliedDues(bookingId, appliedDues.total());
      }
    }

    Map<String, Object> response = new java.util.LinkedHashMap<>();
    response.put("bookingId", bookingId);
    response.put("status", initialStatus);
    response.put("routeOccurrenceId", reservation.routeOccurrenceId());
    response.put("fareEstimate", fareEstimate);
    response.put("appliedDues", appliedDues);
    response.put("totalDue", fareEstimate.add(appliedDues.total()));
    response.put("approvalMode", approvalMode.name());
    response.put("seats", heldSeats);
    response.put("expiresAt", expiresAt);
    response.put(
        "secondsRemaining",
        expiresAt == null
            ? null
            : Math.max(0, java.time.Duration.between(clock.instant(), expiresAt).toSeconds()));
    idempotencyKeys.storeResponse(idempotencyKey, responseBody(response), 200);
    notifications.notifyUser(
        app.appUserId(),
        confirmed ? "BOOKING_CONFIRMED" : "BOOKING_REQUESTED",
        confirmed ? "Booking confirmed" : "Request sent",
        confirmed
            ? "Your seat has been reserved and the booking is confirmed."
            : "Your driver has 30 minutes to reply. Nothing is charged unless they accept.",
        Map.of("bookingId", String.valueOf(bookingId)));
    return response;
  }

  @Override
  @Transactional
  public Map<String, Object> transition(long bookingId, BookingStatusTransitionRequest req) {
    String toStatus = normalizeStatus(req.status());
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    String fromStatus =
        bookings
            .findStatusForUpdateByIdAndPassengerAppUserId(bookingId, app.appUserId())
            .orElseThrow(() -> new java.util.NoSuchElementException("Booking not found"));
    updateBookingStatus(bookingId, app.appUserId(), fromStatus, toStatus, req.reason());
    if (CANCELLED.equals(toStatus)) {
      // The car being already in motion is the whole difference. Before the wheels moved the hold
      // is simply released; after they moved, she leaves a seat nobody else can take on a trip that
      // is already running, and P26 prices that at a fifth of her fare.
      boolean afterStart = bookings.isTripStartedForBooking(bookingId);
      if (afterStart) {
        penalties.assessPassengerCancelAfterStart(
            bookingId, bookings.findTripId(bookingId).orElse(null));
      } else {
        payments.voidForBooking(bookingId, "PASSENGER_CANCELLED");
      }
      // Any fee this booking was carrying rides on to the next one rather than being cleared by a
      // checkout that never charged.
      penalties.releaseDuesForBooking(bookingId);
      // The seat goes back to the car the moment the booking stops holding it. A leaked hold
      // removes inventory permanently and silently — the driver sees a full trip and nobody can
      // say why.
      seatHolds.release(bookingId);
      // Recorded as a free cancel if her driver was late, and as an ordinary one otherwise. The
      // grace row already knows which; the client is never asked.
      tripLifecycle.resolveLateGraceOnCancel(bookingId);
      bookings
          .findDriverAppUserIdForPassengerBooking(bookingId, app.appUserId())
          .ifPresent(
              driverAppUserId ->
                  notifications.notifyUser(
                      driverAppUserId,
                      "BOOKING_CANCELLED",
                      "Booking cancelled",
                      "A passenger has cancelled their booking.",
                      Map.of("bookingId", String.valueOf(bookingId))));
    }
    return Map.of("bookingId", bookingId, "status", toStatus);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PassengerBookingSummaryResponse> listPassengerBookings() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return bookings.findPassengerBookings(app.appUserId()).stream().map(this::toSummary).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public PassengerBookingDetailResponse getPassengerBooking(long bookingId) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return bookings
        .findPassengerBookingDetail(app.appUserId(), bookingId)
        .map(this::toDetail)
        .orElseThrow(() -> new java.util.NoSuchElementException("Booking not found"));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<PassengerBookingDetailResponse> getCurrentPassengerTrip() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return bookings.findCurrentPassengerTrip(app.appUserId()).map(this::toDetail);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PassengerBookingSummaryResponse> listPassengerTripHistory() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return bookings.findPassengerTripHistory(app.appUserId()).stream()
        .map(this::toSummary)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DriverBookingRequestResponse> listDriverBookingRequests(Long tripId) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return bookings.findDriverBookingRequests(app.appUserId(), tripId).stream()
        .map(this::toDriverBookingRequest)
        .toList();
  }

  @Override
  @Transactional
  public Map<String, Object> approveByDriver(long bookingId) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    String fromStatus =
        bookings
            .findStatusForUpdateByIdAndDriverAppUserId(bookingId, app.appUserId())
            .orElseThrow(() -> new java.util.NoSuchElementException("Booking not found"));
    // A request the driver takes too long over is gone, and approving it would confirm a seat the
    // rider has already been told she lost.
    if (bookings.isRequestExpired(bookingId)) {
      throw new com.routeshare.common.errors.GateConflictException(
          "REQUEST_EXPIRED",
          "This request lapsed before you replied, and the seat has been released.",
          "/driver/trips");
    }
    updateBookingStatus(
        bookingId, app.appUserId(), fromStatus, CONFIRMED, "Driver approved booking");
    bookings.clearExpiry(bookingId);
    // A booking that needed approval reaches CONFIRMED here instead of at creation, so this is
    // where its occurrence earns a trip — and where the card is finally held.
    bookings
        .findRouteOccurrenceId(bookingId)
        .ifPresent(tripLifecycle::ensureTripForBookedOccurrence);
    tripLifecycle.openLateGraceForBooking(bookingId);
    bookings
        .findFareAndPaymentMethod(bookingId)
        .ifPresent(
            row -> {
              payments.authorizeForBooking(
                  bookingId, row.getPaymentMethodId(), row.getFareEstimate());
              var carried = penalties.applyOutstandingDues(row.getPassengerAppUserId(), bookingId);
              if (carried.total().signum() > 0) {
                bookings.recordAppliedDues(bookingId, carried.total());
              }
            });
    notifyPassenger(
        bookingId,
        "BOOKING_CONFIRMED",
        "Booking confirmed",
        "The driver has approved your booking.");
    return Map.of("bookingId", bookingId, "status", CONFIRMED);
  }

  @Override
  @Transactional
  public Map<String, Object> declineByDriver(long bookingId, String reason) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    String fromStatus =
        bookings
            .findStatusForUpdateByIdAndDriverAppUserId(bookingId, app.appUserId())
            .orElseThrow(() -> new java.util.NoSuchElementException("Booking not found"));
    updateBookingStatus(bookingId, app.appUserId(), fromStatus, REJECTED, reason);
    payments.voidForBooking(bookingId, "DRIVER_DECLINED");
    penalties.releaseDuesForBooking(bookingId);
    seatHolds.release(bookingId);
    notifyPassenger(
        bookingId,
        "BOOKING_DECLINED",
        "Booking declined",
        "The driver was unable to accept your booking.");
    return Map.of("bookingId", bookingId, "status", REJECTED);
  }

  private void notifyPassenger(long bookingId, String type, String title, String body) {
    bookings
        .findPassengerAppUserId(bookingId)
        .ifPresent(
            passengerAppUserId ->
                notifications.notifyUser(
                    passengerAppUserId,
                    type,
                    title,
                    body,
                    Map.of("bookingId", String.valueOf(bookingId))));
  }

  private void updateBookingStatus(
      long bookingId, long actorAppUserId, String fromStatus, String toStatus, String reason) {
    assertTransition(fromStatus, toStatus);
    int updated = bookings.updateStatus(bookingId, toStatus);
    if (updated != 1) {
      throw new IllegalStateException("Booking status update failed");
    }
    statusHistory.recordTransition(
        bookingId,
        fromStatus,
        toStatus,
        actorAppUserId,
        transitionReason(reason, fromStatus, toStatus));
  }

  private PassengerBookingSummaryResponse toSummary(BookingRepository.PassengerBookingRow row) {
    return new PassengerBookingSummaryResponse(
        row.getBookingId(),
        row.getRoutePlanId(),
        row.getRouteOccurrenceId(),
        row.getTripId(),
        row.getOriginLabel(),
        row.getDestinationLabel(),
        row.getDepartureTime(),
        row.getSeats(),
        row.getBookingStatus(),
        row.getTripStatus(),
        row.getPassengerTripStatus(),
        row.getFareEstimate(),
        row.getPaymentStatus(),
        row.getCreatedAt());
  }

  private PassengerBookingDetailResponse toDetail(BookingRepository.PassengerBookingRow row) {
    return new PassengerBookingDetailResponse(
        row.getBookingId(),
        row.getRoutePlanId(),
        row.getRouteOccurrenceId(),
        row.getTripId(),
        row.getOriginLabel(),
        row.getDestinationLabel(),
        row.getDepartureTime(),
        row.getSeats(),
        row.getBookingStatus(),
        row.getTripStatus(),
        row.getPassengerTripStatus(),
        row.getFareEstimate(),
        row.getPaymentStatus(),
        new PassengerBookingDetailResponse.Payment(
            row.getPaymentMethod(),
            row.getPaymentStatus(),
            row.getAuthorizedAt(),
            row.getCapturedAt(),
            row.getPaymentAmount(),
            row.getCardLast4()),
        row.getPickupLatitude(),
        row.getPickupLongitude(),
        row.getDropoffLatitude(),
        row.getDropoffLongitude(),
        row.getPickupRouteFraction(),
        row.getDropoffRouteFraction(),
        row.getCreatedAt());
  }

  private DriverBookingRequestResponse toDriverBookingRequest(
      BookingRepository.DriverBookingRequestRow row) {
    return new DriverBookingRequestResponse(
        row.getBookingId(),
        row.getRoutePlanId(),
        row.getRouteOccurrenceId(),
        row.getTripId(),
        row.getPassengerAppUserId(),
        row.getPassengerName(),
        row.getSeats(),
        row.getStatus(),
        row.getFareEstimate(),
        row.getPickupLatitude(),
        row.getPickupLongitude(),
        row.getDropoffLatitude(),
        row.getDropoffLongitude(),
        row.getCreatedAt());
  }

  private String normalizeStatus(String status) {
    String normalized = status.trim().toUpperCase(java.util.Locale.ROOT);
    if (!ALLOWED_TRANSITIONS.containsKey(normalized)) {
      throw new IllegalArgumentException("Unsupported booking status: " + status);
    }
    return normalized;
  }

  private void assertTransition(String fromStatus, String toStatus) {
    if (!ALLOWED_TRANSITIONS.getOrDefault(fromStatus, Set.of()).contains(toStatus)) {
      throw new IllegalStateException(
          "Invalid booking transition from " + fromStatus + " to " + toStatus);
    }
  }

  private String transitionReason(String reason, String fromStatus, String toStatus) {
    if (StringUtils.hasText(reason)) {
      return reason.trim();
    }
    return "Booking status changed from " + fromStatus + " to " + toStatus;
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
