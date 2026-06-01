package com.routeshare.trip.service.impl;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.trip.domain.PassengerTripStateMachine;
import com.routeshare.trip.domain.TripStateMachine;
import com.routeshare.trip.dto.request.PassengerTripStateTransitionRequest;
import com.routeshare.trip.dto.request.TripTransitionRequest;
import com.routeshare.trip.repository.PassengerTripStateRepository;
import com.routeshare.trip.repository.TripRepository;
import com.routeshare.trip.service.TripService;
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
    trips.updateStatus(tripId, req.status());
    return Map.of("tripId", tripId, "status", req.status().name());
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
    return Map.of("tripId", tripId, "bookingId", bookingId, "status", req.status().name());
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
