package com.routeshare.trip.service;

import com.routeshare.trip.dto.request.PassengerTripStateTransitionRequest;
import com.routeshare.trip.dto.request.PreTripChecklistRequest;
import com.routeshare.trip.dto.request.TripTransitionRequest;
import com.routeshare.trip.dto.response.DriverTripResponse;
import java.util.List;
import java.util.Map;

public interface TripService {
  Map<String, Object> transition(long tripId, TripTransitionRequest req);

  Map<String, Object> transitionPassengerState(
      long tripId, long bookingId, PassengerTripStateTransitionRequest req);

  List<DriverTripResponse> listDriverTrips();

  DriverTripResponse getDriverTrip(long tripId);

  Map<String, Object> recordPreTripChecklist(long tripId, PreTripChecklistRequest req);

  Map<String, Object> markArrivedPickup(long tripId);
}
