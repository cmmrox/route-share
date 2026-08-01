package com.routeshare.platform.dto.response;

import java.time.Instant;

public record PolicySettingResponse(
    String policyKey, String value, String valueType, String description, Instant updatedAt) {}
