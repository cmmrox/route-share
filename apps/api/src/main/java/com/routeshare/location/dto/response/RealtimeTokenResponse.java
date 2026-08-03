package com.routeshare.location.dto.response;

import java.time.Instant;

public record RealtimeTokenResponse(
    String token, Instant expiresAt, String websocketUrl, String sseUrl) {}
