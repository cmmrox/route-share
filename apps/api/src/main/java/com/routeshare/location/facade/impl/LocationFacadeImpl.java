package com.routeshare.location.facade.impl;

import com.routeshare.location.cache.LatestLocationCache;
import com.routeshare.location.facade.LocationFacade;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocationFacadeImpl implements LocationFacade {
  private final LatestLocationCache latest;

  @Override
  public Optional<Snapshot> latestForTrip(long tripId) {
    return latest
        .findByTripId(tripId)
        .filter(snapshot -> snapshot.latitude() != null && snapshot.longitude() != null)
        .map(snapshot -> new Snapshot(snapshot.latitude(), snapshot.longitude()));
  }
}
