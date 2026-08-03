package com.routeshare.notification.push;

import java.util.Map;

/**
 * Sends a push to a single device token. The real {@link
 * com.routeshare.notification.push.impl.FcmPushAdapter} delivers via Firebase Cloud Messaging when
 * enabled; otherwise {@link com.routeshare.notification.push.impl.LoggingPushAdapter} logs so local
 * flows work without Firebase credentials.
 */
public interface PushNotificationPort {

  boolean enabled();

  PushResult send(PushMessage message);

  record PushMessage(
      String token, String title, String body, Map<String, String> data, boolean highPriority) {
    public PushMessage(String token, String title, String body, Map<String, String> data) {
      this(token, title, body, data, false);
    }
  }

  record PushResult(boolean success, String messageId, String error) {
    public static PushResult ok(String messageId) {
      return new PushResult(true, messageId, null);
    }

    public static PushResult failed(String error) {
      return new PushResult(false, null, error);
    }
  }
}
