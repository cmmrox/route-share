package com.routeshare.location.service.impl;

import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.location.domain.EtaCalculator;
import com.routeshare.location.dto.request.ApproachPositionRequest;
import com.routeshare.location.dto.response.ApproachResponse;
import com.routeshare.location.repository.ApproachSessionRepository;
import com.routeshare.location.service.ApproachService;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApproachServiceImpl implements ApproachService {
  private final CurrentUserProvider current;
  private final IdentityFacade identity;
  private final ApproachSessionRepository sessions;
  private final EtaCalculator eta;
  private final MeterRegistry meters;
  private final Clock clock;

  @Value("${routeshare.location.approach-radius-meters:500}")
  private double approachRadiusMeters;

  @Override
  @Transactional
  public void evaluateForTrip(long tripId) {
    sessions
        .nextPickup(tripId)
        .filter(row -> row.getDistanceMeters() <= approachRadiusMeters)
        .ifPresent(
            row -> {
              if (sessions.open(tripId, row.getBookingId()) > 0) {
                meters
                    .counter("routeshare_approach_sessions_total", "result", "opened")
                    .increment();
              }
            });
  }

  @Override
  @Transactional(readOnly = true)
  public ApproachResponse driverApproach(long tripId) {
    long appUserId = appUserId();
    if (!sessions.driverOwnsTrip(tripId, appUserId)) {
      throw new AccessDeniedException("Trip does not belong to current driver");
    }
    return sessions
        .driverApproach(tripId, appUserId)
        .map(this::response)
        .orElseGet(ApproachResponse::inactive);
  }

  @Override
  @Transactional(readOnly = true)
  public ApproachResponse passengerApproach(long bookingId) {
    long appUserId = appUserId();
    if (!sessions.passengerOwnsBooking(bookingId, appUserId)) {
      throw new AccessDeniedException("Booking does not belong to current passenger");
    }
    return sessions
        .passengerApproach(bookingId, appUserId)
        .map(this::response)
        .orElseGet(ApproachResponse::inactive);
  }

  @Override
  @Transactional
  public ApproachResponse updatePassengerPosition(long bookingId, ApproachPositionRequest request) {
    long appUserId = appUserId();
    if (!sessions.passengerOwnsBooking(bookingId, appUserId)) {
      throw new AccessDeniedException("Booking does not belong to current passenger");
    }
    int updated =
        sessions.updateRiderPosition(
            bookingId, appUserId, request.latitude(), request.longitude(), Instant.now(clock));
    if (updated == 0) {
      throw new GateConflictException(
          "APPROACH_NOT_ACTIVE",
          "Passenger position is accepted only during an active pickup approach.",
          "/passenger/bookings/" + bookingId);
    }
    return passengerApproach(bookingId);
  }

  @Override
  @Transactional
  public int closeStaleSessions(int batchSize) {
    Instant now = Instant.now(clock);
    int closed = 0;
    for (Long id :
        sessions.staleSessionIds(now.minus(Duration.ofMinutes(15)), Math.min(batchSize, 500))) {
      closed += sessions.closeAndDeleteRiderPosition(id, now);
    }
    if (closed > 0) {
      meters.counter("routeshare_approach_sessions_total", "result", "closed").increment(closed);
    }
    return closed;
  }

  private ApproachResponse response(ApproachSessionRepository.ApproachRow row) {
    var pickup =
        new ApproachResponse.PickupPoint(
            row.getPickupLabel() == null ? "Pickup point" : row.getPickupLabel(),
            row.getPickupDescription(),
            row.getPickupSideHint(),
            row.getPickupLatitude(),
            row.getPickupLongitude());
    ApproachResponse.Position counterparty =
        row.getCounterpartyLatitude() == null || row.getCounterpartyAt() == null
            ? null
            : new ApproachResponse.Position(
                row.getCounterpartyLatitude(),
                row.getCounterpartyLongitude(),
                Math.max(
                    0, Duration.between(row.getCounterpartyAt(), Instant.now(clock)).toSeconds()));
    var vehicle =
        new ApproachResponse.Vehicle(
            row.getVehicleMake(), row.getVehicleColour(), row.getVehiclePlate());
    Double distance = row.getDistanceMeters();
    Long etaSeconds =
        distance == null
            ? null
            : eta.etaSeconds(
                distance, row.getSpeedMps() == null ? null : row.getSpeedMps().doubleValue());
    return new ApproachResponse(true, pickup, counterparty, distance, etaSeconds, vehicle);
  }

  private long appUserId() {
    return identity.upsertFromToken(current.requireCurrentUser()).appUserId();
  }
}
