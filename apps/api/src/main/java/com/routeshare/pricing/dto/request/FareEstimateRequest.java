package com.routeshare.pricing.dto.request;

import jakarta.validation.constraints.Min;

public record FareEstimateRequest(@Min(0) long distanceMeters) {}
