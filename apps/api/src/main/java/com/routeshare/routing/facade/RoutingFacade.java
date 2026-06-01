package com.routeshare.routing.facade;

import java.util.Optional;

public interface RoutingFacade {
  Optional<Double> reserveSeatsAndReturnRouteLength(long routePlanId, int seats);
}
