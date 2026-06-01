package com.routeshare.routing.facade;

import java.util.Optional;

public interface RoutingFacade {
  Optional<RouteReservation> reserveSeatsAndReturnRouteLength(long routeOccurrenceId, int seats);
}
