package com.routeshare.notification.service;

import java.util.Map;

public interface RealtimeDeliveryService {
  DeliveryChannel deliver(
      long appUserId,
      String eventType,
      String title,
      String body,
      Map<String, String> data,
      boolean tripCritical);

  enum DeliveryChannel {
    REALTIME,
    FCM_HIGH_PRIORITY,
    FCM_NORMAL_PRIORITY,
    UNDELIVERABLE
  }
}
