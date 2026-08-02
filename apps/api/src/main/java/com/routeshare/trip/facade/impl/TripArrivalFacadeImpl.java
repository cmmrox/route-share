package com.routeshare.trip.facade.impl;

import com.routeshare.trip.facade.TripArrivalFacade;
import com.routeshare.trip.service.ArrivalDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TripArrivalFacadeImpl implements TripArrivalFacade {

  private final ArrivalDetectionService arrivals;

  @Override
  public int onDriverLocation(long tripId) {
    return arrivals.onDriverLocation(tripId);
  }
}
