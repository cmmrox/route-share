package com.routeshare.location.dto.response;

import com.routeshare.location.domain.LocationRejectionReason;
import java.util.List;

public record LocationBatchUpdateResponse(
    int accepted,
    List<RejectedSample> rejected,
    TripProgressResponse progress,
    LocationPolicyResponse policy) {
  public record RejectedSample(String sampleId, LocationRejectionReason reason) {}
}
