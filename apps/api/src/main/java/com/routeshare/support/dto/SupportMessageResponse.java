package com.routeshare.support.dto;

import java.time.Instant;

public record SupportMessageResponse(long id, String senderRole, String body, Instant createdAt) {}
