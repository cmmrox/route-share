package com.routeshare.location.facade;

import com.routeshare.location.domain.LocationConfidence;
import com.routeshare.location.dto.response.TripProgressResponse;
import java.util.List;

public interface TripProgressFacade {
  TripProgressResponse progressFor(long tripId);

  List<CandidateTrip> candidateTripsNear(double latitude, double longitude, double radiusMeters);

  record CandidateTrip(
      long tripId,
      double routeFraction,
      LocationConfidence confidence,
      double latitude,
      double longitude) {}
}
