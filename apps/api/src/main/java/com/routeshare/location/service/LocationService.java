package com.routeshare.location.service;

import com.routeshare.location.dto.request.DriverLocationUpdateRequest;
import com.routeshare.location.dto.request.LocationUpdateRequest;
import com.routeshare.location.dto.response.AdminLiveTripResponse;
import com.routeshare.location.dto.response.LocationUpdateResponse;
import com.routeshare.location.dto.response.PassengerLiveTripStateResponse;
import java.util.List;
import java.util.Map;

public interface LocationService {
  Map<String, Object> update(LocationUpdateRequest req);

  LocationUpdateResponse ingestDriverLocation(Long tripId, DriverLocationUpdateRequest request);

  PassengerLiveTripStateResponse getPassengerLiveTripState(Long tripId);

  List<AdminLiveTripResponse> getAdminLiveTrips(int limit);
}
