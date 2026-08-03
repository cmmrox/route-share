package com.routeshare.location.facade.impl;

import com.routeshare.location.domain.LocationConfidence;
import com.routeshare.location.facade.TripProgressFacade;
import com.routeshare.location.repository.LocationPipelineRepository;
import com.routeshare.location.service.LocationPipelineService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TripProgressFacadeImpl implements TripProgressFacade {
  private final LocationPipelineService service;
  private final LocationPipelineRepository progress;

  @Override
  public com.routeshare.location.dto.response.TripProgressResponse progressFor(long tripId) {
    return service.progress(tripId);
  }

  @Override
  public List<CandidateTrip> candidateTripsNear(
      double latitude, double longitude, double radiusMeters) {
    if (radiusMeters <= 0 || radiusMeters > 100_000) {
      throw new IllegalArgumentException("radiusMeters must be between 0 and 100000");
    }
    return progress.candidateTripsNear(latitude, longitude, radiusMeters).stream()
        .map(
            row ->
                new CandidateTrip(
                    row.getTripId(),
                    row.getRouteFraction().doubleValue(),
                    LocationConfidence.valueOf(row.getConfidence()),
                    row.getLatitude(),
                    row.getLongitude()))
        .toList();
  }
}
