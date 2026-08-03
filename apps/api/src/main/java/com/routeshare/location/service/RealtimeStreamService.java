package com.routeshare.location.service;

import java.util.Map;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface RealtimeStreamService {
  SseEmitter openSse(long appUserId, String connectionId);

  boolean deliver(long appUserId, String eventType, Map<String, String> payload);
}
