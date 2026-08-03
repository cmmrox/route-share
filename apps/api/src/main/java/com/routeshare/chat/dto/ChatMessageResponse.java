package com.routeshare.chat.dto;

import java.time.Instant;

public record ChatMessageResponse(
    long id, long senderAppUserId, String body, Instant sentAt, Instant readByCounterpartyAt) {}
