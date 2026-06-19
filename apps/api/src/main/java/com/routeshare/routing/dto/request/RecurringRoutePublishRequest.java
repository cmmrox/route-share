package com.routeshare.routing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * Publishes a recurring route. {@code daysOfWeek} uses {@code MON..SUN}; an empty list means daily.
 * Occurrences are generated from {@code firstDepartureTime}'s time-of-day across matching days up
 * to {@code endAt} (or {@code horizonDays}).
 */
public record RecurringRoutePublishRequest(
    @NotNull Long vehicleId,
    @NotBlank String originLabel,
    @NotBlank String destinationLabel,
    @NotNull @Size(min = 2, max = 500) List<@Valid CoordinateRequest> coordinates,
    @Min(1) int availableSeats,
    @NotNull Instant firstDepartureTime,
    Instant endAt,
    List<String> daysOfWeek,
    Integer horizonDays) {}
