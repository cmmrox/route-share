package com.routeshare.location.facade.impl;

import com.routeshare.location.facade.RealtimeChannelFacade;
import com.routeshare.location.service.RealtimeStreamService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealtimeChannelFacadeImpl implements RealtimeChannelFacade {
  private final RealtimeStreamService realtime;

  @Override
  public boolean deliver(long appUserId, String eventType, Map<String, String> payload) {
    return realtime.deliver(appUserId, eventType, payload);
  }
}
