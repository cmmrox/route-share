package com.routeshare.trip.dto.request;

import com.routeshare.trip.domain.TripStatus;
import jakarta.validation.constraints.NotNull;

public record TripTransitionRequest(@NotNull TripStatus status) {}
