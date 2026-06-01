package com.routeshare.trip.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.trip.domain.TripStateMachine;
import com.routeshare.trip.dto.request.TripTransitionRequest;
import com.routeshare.trip.repository.TripRepository;
import com.routeshare.trip.service.TripService;
import java.util.Map;
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
  private final TripStateMachine stateMachine = new TripStateMachine();

  @Transactional
  public Map<String, Object> transition(long tripId, TripTransitionRequest req) {
    var currentUser = current.requireCurrentUser();
    var app = identityFacade.upsertFromToken(currentUser);
    boolean owner = trips.isOwnedByDriverAppUser(tripId, app.appUserId());
    if (!owner && currentUser.roles().stream().noneMatch(this::isAdminRole)) {
      throw new AccessDeniedException("Trip does not belong to current user");
    }

    var currentStatus = trips.findStatusForUpdate(tripId);
    stateMachine.assertTransition(currentStatus, req.status());
    trips.updateStatus(tripId, req.status());
    return Map.of("tripId", tripId, "status", req.status().name());
  }

  private boolean isAdminRole(String role) {
    return role.equals("ADMIN") || role.equals("OPS_ADMIN") || role.equals("SUPER_ADMIN");
  }
}
