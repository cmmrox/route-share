package com.routeshare.chat.dto;

import jakarta.validation.constraints.Positive;

public record ChatReadRequest(@Positive long upToMessageId) {}
