package com.routeshare.support.dto;

import java.time.Instant;
import java.util.List;

public record SupportTicketResponse(
    long id,
    String subject,
    String category,
    String status,
    String priority,
    Instant createdAt,
    Instant updatedAt,
    List<SupportMessageResponse> messages) {}
