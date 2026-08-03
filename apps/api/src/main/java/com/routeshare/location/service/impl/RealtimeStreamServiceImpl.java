package com.routeshare.location.service.impl;

import com.routeshare.location.service.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class RealtimeStreamServiceImpl implements RealtimeStreamService {
  private final SimpMessagingTemplate messaging;
  private final SimpUserRegistry users;
  private final RealtimeChannelService channels;
  private final Map<Long, Map<String, SseEmitter>> emitters = new ConcurrentHashMap<>();

  @Override
  public SseEmitter openSse(long appUserId, String connectionId) {
    SseEmitter emitter = new SseEmitter(120_000L);
    emitters
        .computeIfAbsent(appUserId, ignored -> new ConcurrentHashMap<>())
        .put(connectionId, emitter);
    channels.connect(appUserId, connectionId, "SSE");
    Runnable cleanup = () -> remove(appUserId, connectionId);
    emitter.onCompletion(cleanup);
    emitter.onTimeout(cleanup);
    emitter.onError(ignored -> cleanup.run());
    return emitter;
  }

  @Override
  public boolean deliver(long appUserId, String eventType, Map<String, String> payload) {
    boolean delivered = false;
    Map<String, SseEmitter> userEmitters = emitters.getOrDefault(appUserId, Map.of());
    for (var entry : List.copyOf(userEmitters.entrySet())) {
      try {
        entry
            .getValue()
            .send(SseEmitter.event().name(eventType).data(payload == null ? Map.of() : payload));
        delivered = true;
      } catch (IOException ex) {
        remove(appUserId, entry.getKey());
      }
    }
    if (users.getUser(String.valueOf(appUserId)) != null) {
      messaging.convertAndSendToUser(
          String.valueOf(appUserId),
          "/queue/realtime",
          Map.of("type", eventType, "data", payload == null ? Map.of() : payload));
      delivered = true;
    }
    return delivered;
  }

  private void remove(long appUserId, String connectionId) {
    Map<String, SseEmitter> user = emitters.get(appUserId);
    if (user != null) {
      user.remove(connectionId);
      if (user.isEmpty()) {
        emitters.remove(appUserId);
      }
    }
    channels.disconnect(connectionId);
  }
}
