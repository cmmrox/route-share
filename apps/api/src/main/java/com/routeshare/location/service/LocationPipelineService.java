package com.routeshare.location.service;

import com.routeshare.location.dto.request.LocationBatchUpdateRequest;
import com.routeshare.location.dto.response.*;

public interface LocationPipelineService {
  LocationBatchUpdateResponse ingest(long tripId, LocationBatchUpdateRequest request);

  TripProgressResponse progress(long tripId);

  TripProgressResponse driverProgress(long tripId);

  LocationPolicyResponse policy(Integer batteryPercent);
}
