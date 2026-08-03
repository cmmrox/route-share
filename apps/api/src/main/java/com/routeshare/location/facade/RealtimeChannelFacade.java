package com.routeshare.location.facade;

import java.util.Map;

public interface RealtimeChannelFacade {
  boolean deliver(long appUserId, String eventType, Map<String, String> payload);
}
