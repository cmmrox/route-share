package com.routeshare.driver.dto.response;

import java.time.Instant;

/** The deactivation as the driver and the admin both see it (board D34). */
public record DriverDeactivationResponse(
    Long deactivationId,
    long driverProfileId,
    String reason,
    String caseRef,
    Instant deactivatedAt,
    Instant reinstatedAt,
    boolean active) {}
