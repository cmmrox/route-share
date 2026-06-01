package com.routeshare.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookingStatusTransitionRequest(
    @NotBlank @Size(max = 32) String status, @Size(max = 500) String reason) {}
