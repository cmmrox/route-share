package com.routeshare.location.service;

import com.routeshare.location.dto.request.ApproachPositionRequest;
import com.routeshare.location.dto.response.ApproachResponse;

public interface ApproachService {
  void evaluateForTrip(long tripId);

  ApproachResponse driverApproach(long tripId);

  ApproachResponse passengerApproach(long bookingId);

  ApproachResponse updatePassengerPosition(long bookingId, ApproachPositionRequest request);

  int closeStaleSessions(int batchSize);
}
