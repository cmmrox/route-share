package com.routeshare.trip.service;

import com.routeshare.trip.dto.request.TripTransitionRequest;
import java.util.Map;

public interface TripService {
  Map<String, Object> transition(long tripId, TripTransitionRequest req);
}
