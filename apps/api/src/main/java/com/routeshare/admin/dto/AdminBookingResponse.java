package com.routeshare.admin.dto;

import java.math.BigDecimal;

public record AdminBookingResponse(
    long id,
    Long routePlanId,
    Long routeOccurrenceId,
    Long passengerAppUserId,
    Integer seats,
    String status,
    BigDecimal fareEstimate) {}
