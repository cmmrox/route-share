package com.routeshare.chat.controller;

import com.routeshare.chat.dto.ChatMessagesResponse;
import com.routeshare.chat.service.ChatService;
import com.routeshare.common.web.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/bookings/{bookingId}/chat")
@PreAuthorize("hasAnyRole('SUPPORT_AGENT','OPS_ADMIN','ADMIN','SUPER_ADMIN')")
public class AdminChatController {
  private final ChatService chat;

  public AdminChatController(ChatService chat) {
    this.chat = chat;
  }

  @GetMapping("/messages")
  ApiResponse<ChatMessagesResponse> messages(
      @PathVariable long bookingId,
      @RequestParam String reason,
      @RequestParam(defaultValue = "0") long since,
      @RequestParam(defaultValue = "50") int limit) {
    return ApiResponse.ok(chat.adminMessages(bookingId, reason, since, limit));
  }
}
