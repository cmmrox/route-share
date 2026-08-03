package com.routeshare.chat.controller;

import com.routeshare.chat.dto.*;
import com.routeshare.chat.service.ChatService;
import com.routeshare.common.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/chat")
@PreAuthorize("isAuthenticated()")
public class ChatController {
  private final ChatService chat;

  public ChatController(ChatService chat) {
    this.chat = chat;
  }

  @GetMapping
  ApiResponse<ChatThreadResponse> thread(@PathVariable long bookingId) {
    return ApiResponse.ok(chat.thread(bookingId));
  }

  @GetMapping("/messages")
  ApiResponse<ChatMessagesResponse> messages(
      @PathVariable long bookingId,
      @RequestParam(defaultValue = "0") long since,
      @RequestParam(defaultValue = "50") int limit) {
    return ApiResponse.ok(chat.messages(bookingId, since, limit));
  }

  @PostMapping("/messages")
  ApiResponse<ChatMessageResponse> send(
      @PathVariable long bookingId,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody ChatMessageRequest request) {
    return ApiResponse.ok(chat.send(bookingId, idempotencyKey, request));
  }

  @PostMapping("/read")
  ApiResponse<Integer> read(
      @PathVariable long bookingId, @Valid @RequestBody ChatReadRequest request) {
    return ApiResponse.ok(chat.markRead(bookingId, request));
  }
}
