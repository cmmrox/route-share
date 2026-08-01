package com.routeshare.vehicle.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ChosenRateRequest(@NotNull @Positive BigDecimal ratePerKm) {}
