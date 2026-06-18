package com.routeshare.notification.push.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.routeshare.notification.push.PushNotificationPort;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Real FCM delivery via the Firebase Admin SDK. Active when {@code routeshare.push.enabled=true}.
 */
@Component
@ConditionalOnProperty(prefix = "routeshare.push", name = "enabled", havingValue = "true")
public class FcmPushAdapter implements PushNotificationPort {
  private static final Logger log = LoggerFactory.getLogger(FcmPushAdapter.class);

  private final FirebaseMessaging messaging;

  public FcmPushAdapter(FirebaseMessaging messaging) {
    this.messaging = messaging;
  }

  @Override
  public boolean enabled() {
    return true;
  }

  @Override
  public PushResult send(PushMessage message) {
    var builder =
        Message.builder()
            .setToken(message.token())
            .setNotification(
                Notification.builder().setTitle(message.title()).setBody(message.body()).build());
    if (message.data() != null) {
      for (Map.Entry<String, String> e : message.data().entrySet()) {
        if (e.getValue() != null) {
          builder.putData(e.getKey(), e.getValue());
        }
      }
    }
    try {
      return PushResult.ok(messaging.send(builder.build()));
    } catch (FirebaseMessagingException e) {
      log.warn("fcm_send_failed code={} message={}", e.getMessagingErrorCode(), e.getMessage());
      return PushResult.failed(String.valueOf(e.getMessagingErrorCode()));
    }
  }
}
