package com.routeshare.chat.dto;

import java.util.List;

public record ChatMessagesResponse(List<ChatMessageResponse> items, long nextCursor) {}
