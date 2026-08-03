package com.routeshare.location.facade;

import java.util.Optional;

public interface LocationFacade {
  Optional<Snapshot> latestForTrip(long tripId);

  record Snapshot(double latitude, double longitude) {}
}
