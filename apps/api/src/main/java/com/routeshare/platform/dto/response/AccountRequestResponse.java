package com.routeshare.platform.dto.response;

import java.time.Instant;

public record AccountRequestResponse(
    long id,
    long appUserId,
    String kind,
    String status,
    Instant requestedAt,
    int receiptRetentionYears,
    String note) {}
