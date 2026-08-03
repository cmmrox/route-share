package com.routeshare.chat.dto;

import java.time.Instant;
import java.util.List;

public record ChatThreadResponse(
    long id,
    long bookingId,
    String state,
    Instant openedAt,
    Instant closesAt,
    ChatParticipantResponse counterparty,
    List<String> quickReplies,
    boolean supportReadable) {}
