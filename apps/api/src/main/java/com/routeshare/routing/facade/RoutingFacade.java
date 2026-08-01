package com.routeshare.routing.facade;

import java.util.Optional;

public interface RoutingFacade {
  Optional<RouteReservation> reserveSeatsAndReturnRouteLength(long routeOccurrenceId, int seats);

  /**
   * The vehicle and route length behind a published trip — everything pricing needs, nothing more.
   */
  Optional<PriceableTrip> findPriceableTrip(long routeOccurrenceId);

  record PriceableTrip(long routeOccurrenceId, long vehicleId, double routeLengthMeters) {}
}
