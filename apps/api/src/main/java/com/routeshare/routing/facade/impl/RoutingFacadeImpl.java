package com.routeshare.routing.facade.impl;

import com.routeshare.routing.facade.RoutingFacade;
import com.routeshare.routing.repository.RoutePlanRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoutingFacadeImpl implements RoutingFacade {
  private final RoutePlanRepository routes;

  @Override
  public Optional<Double> reserveSeatsAndReturnRouteLength(long routePlanId, int seats) {
    return routes.reserveSeatsAndReturnRouteLength(routePlanId, seats);
  }
}
