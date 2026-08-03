package com.routeshare.trip.facade.impl;

import com.routeshare.trip.facade.TripActivityFacade;
import com.routeshare.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TripActivityFacadeImpl implements TripActivityFacade {

  private final TripRepository trips;

  @Override
  public boolean hasActiveDriverTrip(long appUserId) {
    return trips.hasActiveDriverTrip(appUserId);
  }
}
