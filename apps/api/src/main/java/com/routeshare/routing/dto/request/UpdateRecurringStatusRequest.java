package com.routeshare.routing.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateRecurringStatusRequest(@NotBlank String status) {}
