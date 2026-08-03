package com.routeshare.location.event;

import com.routeshare.location.service.RealtimeChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

@Component
@RequiredArgsConstructor
public class RealtimeSessionListener {
  private final RealtimeChannelService channels;

  @EventListener
  public void connected(SessionConnectedEvent event) {
    SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
    if (headers.getUser() != null && headers.getSessionId() != null) {
      channels.connect(Long.parseLong(headers.getUser().getName()), headers.getSessionId(), "WS");
    }
  }

  @EventListener
  public void disconnected(SessionDisconnectEvent event) {
    channels.disconnect(event.getSessionId());
  }
}
