package com.routeshare.identity.dto.response;

import java.util.Set;

public record AuthMeResponse(
    String subject,
    String email,
    String phone,
    String displayName,
    Set<String> roles,
    boolean hasPassengerProfile,
    boolean hasDriverProfile,
    String driverVerificationStatus,
    Set<String> availableAppModes) {}
