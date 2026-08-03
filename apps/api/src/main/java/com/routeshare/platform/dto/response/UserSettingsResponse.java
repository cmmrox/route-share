package com.routeshare.platform.dto.response;

import java.time.Instant;

public record UserSettingsResponse(
    String theme,
    String language,
    boolean shareLiveLocation,
    boolean showRatingPublicly,
    boolean receiptsByEmail,
    Instant updatedAt) {}
