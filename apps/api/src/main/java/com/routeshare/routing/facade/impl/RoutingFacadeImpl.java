package com.routeshare.routing.facade.impl;

import com.routeshare.routing.facade.RouteReservation;
import com.routeshare.routing.facade.RoutingFacade;
import com.routeshare.routing.repository.RouteOccurrenceRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoutingFacadeImpl implements RoutingFacade {
  private final RouteOccurrenceRepository occurrences;

  @Override
  public Optional<RouteReservation> reserveSeatsAndReturnRouteLength(
      long routeOccurrenceId, int seats) {
    return occurrences
        .reserveSeatsAndReturnRouteLength(routeOccurrenceId, seats)
        .map(
            row ->
                new RouteReservation(
                    row.getRoutePlanId(),
                    routeOccurrenceId,
                    row.getVehicleId(),
                    row.getRouteLengthMeters()));
  }

  @Override
  public Optional<PriceableTrip> findPriceableTrip(long routeOccurrenceId) {
    return occurrences
        .findPriceableTrip(routeOccurrenceId)
        .map(
            row ->
                new PriceableTrip(
                    routeOccurrenceId, row.getVehicleId(), row.getRouteLengthMeters()));
  }
}
