package com.routeshare.vehicle.dto.request;

import com.routeshare.vehicle.domain.VehicleReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VehicleReviewRequest(
    @NotNull VehicleReviewStatus status, @Size(max = 500) String rejectionReason) {}
