package com.routeshare.location.dto.response;

import com.routeshare.location.domain.LocationMode;
import com.routeshare.location.domain.LocationPriority;

public record LocationPolicyResponse(
    int intervalSeconds,
    LocationPriority priority,
    int batchSize,
    LocationMode mode,
    String reason) {}
