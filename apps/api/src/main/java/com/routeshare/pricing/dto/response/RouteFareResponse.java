package com.routeshare.pricing.dto.response;

import com.routeshare.pricing.domain.FareBreakdown;

public record RouteFareResponse(
    long distanceMeters, long durationSeconds, String metricsSource, FareBreakdown fare) {}
