package com.routeshare.routing.dto.response;

import java.time.Instant;
import java.util.List;

public record RecurringRouteResponse(
    long ruleId,
    long routePlanId,
    String originLabel,
    String destinationLabel,
    Instant startAt,
    Instant endAt,
    List<String> daysOfWeek,
    String status) {}
