package com.routeshare.trip.service.impl;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.trip.domain.PassengerTripStateMachine;
import com.routeshare.trip.domain.PassengerTripStatus;
import com.routeshare.trip.domain.TripStateMachine;
import com.routeshare.trip.dto.request.PassengerTripStateTransitionRequest;
import com.routeshare.trip.dto.request.PreTripChecklistRequest;
import com.routeshare.trip.dto.request.TripTransitionRequest;
import com.routeshare.trip.dto.response.DriverTripResponse;
import com.routeshare.trip.repository.PassengerTripStateRepository;
import com.routeshare.trip.repository.TripRepository;
import com.routeshare.trip.service.TripService;
import com.routeshare.trip.service.TripStartWindowService;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final TripRepository trips;
  private final PassengerTripStateRepository passengerStates;
  private final NotificationFacade notifications;
  private final com.routeshare.payment.facade.PaymentFacade payments;
  private final com.routeshare.penalty.facade.PenaltyFacade penalties;
  private final TripStartWindowService startWindows;
  private final com.routeshare.trip.service.PickupWaitService pickupWaits;
  private final com.routeshare.chat.facade.ChatFacade chat;
  private final com.routeshare.platform.service.PolicySettingService policy;
  private final java.time.Clock clock;
  private final TripStateMachine stateMachine = new TripStateMachine();
  private final PassengerTripStateMachine passengerStateMachine = new PassengerTripStateMachine();

  @Override
  @Transactional
  public Map<String, Object> transition(long tripId, TripTransitionRequest req) {
    CurrentUser currentUser = current.requireCurrentUser();
    var app = identityFacade.upsertFromToken(currentUser);
    requireTripOwnerOrAdmin(tripId, currentUser, app.appUserId());

    var currentStatus = trips.findStatusForUpdate(tripId);
    stateMachine.assertTransition(currentStatus, req.status());
    trips.updateStatus(tripId, req.status(), clock.instant());

    // The start window closes in the same transaction as the transition that closed it. Left
    // unresolved, the sweeper would find a trip that is already under way and auto-cancel it —
    // voiding holds that were captured minutes earlier, on a car that is already moving.
    if (req.status() == com.routeshare.trip.domain.TripStatus.STARTED) {
      startWindows.resolveStarted(tripId);
    } else if (req.status() == com.routeshare.trip.domain.TripStatus.CANCELLED) {
      startWindows.resolveCancelled(tripId);
      // Cancelling inside the free window strands everyone booked on it (D30/D31). Outside it,
      // this returns without assessing anything: the seats resell and nobody was let down.
      penalties.assessDriverLateCancellation(tripId);
    }

    // Starting the trip is what charges the cards — the promise on eleven screens. The transition
    // and the intent to capture commit together: a start that is recorded but never charged, or a
    // charge with no trip behind it, are both worse than failing outright.
    //
    // A refused bank does not roll this back. The driver is at the wheel and the other passengers
    // are in the car; that booking is flagged and the trip runs.
    List<com.routeshare.payment.domain.CaptureOutcome> captures =
        req.status() == com.routeshare.trip.domain.TripStatus.STARTED
            ? payments.captureForTripStart(tripId)
            : List.of();

    // A fee carried from an earlier trip is paid when the checkout that carried it is actually
    // charged — not when it was added to the total. A booking that never starts leaves it
    // outstanding, which is the difference between showing a charge and taking one.
    for (var capture : captures) {
      if (capture.result() == com.routeshare.payment.domain.CaptureOutcome.Result.CAPTURED) {
        penalties.settleDuesForBooking(capture.bookingId());
      }
    }

    notifyTripStatus(tripId, req.status());
    return Map.of(
        "tripId", tripId,
        "status", req.status().name(),
        "captures", captures);
  }

  @Override
  @Transactional
  public Map<String, Object> transitionPassengerState(
      long tripId, long bookingId, PassengerTripStateTransitionRequest req) {
    CurrentUser currentUser = current.requireCurrentUser();
    var app = identityFacade.upsertFromToken(currentUser);
    requireTripOwnerOrAdmin(tripId, currentUser, app.appUserId());

    passengerStates.ensureWaitingPickupStateForConfirmedBooking(tripId, bookingId);
    var currentStatus =
        passengerStates
            .findStatusForUpdate(tripId, bookingId)
            .orElseThrow(() -> new NoSuchElementException("Passenger trip state not found"));
    passengerStateMachine.assertTransition(currentStatus, req.status());
    int updated = passengerStates.updateStatus(tripId, bookingId, req.status());
    if (updated != 1) {
      throw new IllegalStateException("Passenger trip state update failed");
    }
    // She is in the car, so the wait is over. Left running it would expire behind her and record a
    // no-show against a passenger the driver has already picked up.
    if (req.status() == PassengerTripStatus.BOARDED) {
      pickupWaits.resolveBoarded(tripId, bookingId);
    } else if (req.status() == PassengerTripStatus.DROPPED_OFF) {
      chat.scheduleClose(
          bookingId,
          clock
              .instant()
              .plus(
                  java.time.Duration.ofHours(
                      policy.integer(
                          com.routeshare.platform.domain.PolicyKey
                              .CHAT_CLOSE_HOURS_AFTER_DROPOFF))));
    }
    return Map.of("tripId", tripId, "bookingId", bookingId, "status", req.status().name());
  }

  @Override
  @Transactional(readOnly = true)
  public List<DriverTripResponse> listDriverTrips() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return trips.findDriverTrips(app.appUserId()).stream().map(this::toDriverTripResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public DriverTripResponse getDriverTrip(long tripId) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return trips
        .findDriverTrip(app.appUserId(), tripId)
        .map(this::toDriverTripResponse)
        .orElseThrow(() -> new NoSuchElementException("Trip not found"));
  }

  @Override
  @Transactional
  public Map<String, Object> recordPreTripChecklist(long tripId, PreTripChecklistRequest req) {
    CurrentUser currentUser = current.requireCurrentUser();
    var app = identityFacade.upsertFromToken(currentUser);
    requireTripOwnerOrAdmin(tripId, currentUser, app.appUserId());
    trips.insertPreTripChecklist(
        tripId,
        app.appUserId(),
        req.vehicleChecked(),
        req.documentsReady(),
        req.routeReviewed(),
        req.notes());
    return Map.of("tripId", tripId, "status", "CHECKLIST_RECORDED");
  }

  @Override
  @Transactional
  public Map<String, Object> markArrivedPickup(long tripId) {
    CurrentUser currentUser = current.requireCurrentUser();
    var app = identityFacade.upsertFromToken(currentUser);
    requireTripOwnerOrAdmin(tripId, currentUser, app.appUserId());
    var currentStatus = trips.findStatusForUpdate(tripId);
    stateMachine.assertTransition(
        currentStatus, com.routeshare.trip.domain.TripStatus.ARRIVED_PICKUP);
    trips.updateStatus(
        tripId, com.routeshare.trip.domain.TripStatus.ARRIVED_PICKUP, clock.instant());
    trips.insertArrivedPickupEvent(tripId, app.appUserId());
    notifyPassengers(
        tripId, "DRIVER_ARRIVED", "Driver arrived", "Your driver has arrived at the pickup point.");
    return Map.of("tripId", tripId, "status", "ARRIVED_PICKUP");
  }

  private void notifyTripStatus(long tripId, com.routeshare.trip.domain.TripStatus status) {
    switch (status) {
      case STARTED ->
          notifyPassengers(tripId, "TRIP_STARTED", "Trip started", "Your trip is now underway.");
      case COMPLETED ->
          notifyPassengers(
              tripId, "TRIP_COMPLETED", "Trip completed", "Your trip has been completed.");
      case CANCELLED ->
          notifyPassengers(
              tripId, "TRIP_CANCELLED", "Trip cancelled", "Your trip has been cancelled.");
      default -> {
        // Other transitions (SCHEDULED/ARRIVED_PICKUP) do not raise a passenger notification here.
      }
    }
  }

  private void notifyPassengers(long tripId, String type, String title, String body) {
    for (Long passengerAppUserId : trips.findConfirmedPassengerAppUserIds(tripId)) {
      if (passengerAppUserId != null) {
        notifications.notifyUser(
            passengerAppUserId, type, title, body, Map.of("tripId", String.valueOf(tripId)));
      }
    }
  }

  private DriverTripResponse toDriverTripResponse(TripRepository.DriverTripRow row) {
    return new DriverTripResponse(
        row.getTripId(),
        row.getRoutePlanId(),
        row.getRouteOccurrenceId(),
        row.getOriginLabel(),
        row.getDestinationLabel(),
        row.getDepartureTime(),
        row.getStatus(),
        row.getConfirmedBookings(),
        row.getBookedSeats(),
        row.getStartedAt(),
        row.getCompletedAt());
  }

  private void requireTripOwnerOrAdmin(long tripId, CurrentUser currentUser, long appUserId) {
    boolean owner = trips.isOwnedByDriverAppUser(tripId, appUserId);
    if (!owner && currentUser.roles().stream().noneMatch(this::isAdminRole)) {
      throw new AccessDeniedException("Trip does not belong to current user");
    }
  }

  private boolean isAdminRole(String role) {
    return role.equals("ADMIN") || role.equals("OPS_ADMIN") || role.equals("SUPER_ADMIN");
  }
}
