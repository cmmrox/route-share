package com.routeshare.location.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.location.dto.response.RealtimeTokenResponse;
import com.routeshare.location.service.RealtimeChannelService;
import com.routeshare.location.service.RealtimeStreamService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/realtime")
@RequiredArgsConstructor
public class RealtimeController {
  private final RealtimeChannelService channels;
  private final RealtimeStreamService realtime;

  @GetMapping("/token")
  ApiResponse<RealtimeTokenResponse> token() {
    return ApiResponse.ok(channels.issueToken());
  }

  @GetMapping("/sse")
  SseEmitter sse(@RequestParam String token) {
    long appUserId = channels.consumeToken(token);
    return realtime.openSse(appUserId, "sse-" + UUID.randomUUID());
  }
}
