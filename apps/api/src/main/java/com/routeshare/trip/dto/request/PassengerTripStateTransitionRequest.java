package com.routeshare.trip.dto.request;

import com.routeshare.trip.domain.PassengerTripStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PassengerTripStateTransitionRequest(
    @NotNull PassengerTripStatus status, @Size(max = 500) String reason) {}
