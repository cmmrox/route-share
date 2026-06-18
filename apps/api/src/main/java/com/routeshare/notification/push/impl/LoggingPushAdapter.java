package com.routeshare.notification.push.impl;

import com.routeshare.notification.push.PushNotificationPort;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Default push adapter used when Firebase is disabled: logs instead of delivering. */
@Component
@ConditionalOnProperty(
    prefix = "routeshare.push",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class LoggingPushAdapter implements PushNotificationPort {
  private static final Logger log = LoggerFactory.getLogger(LoggingPushAdapter.class);

  @Override
  public boolean enabled() {
    return false;
  }

  @Override
  public PushResult send(PushMessage message) {
    log.info(
        "push_log token={} title={} (firebase disabled)", mask(message.token()), message.title());
    return PushResult.ok("log-" + UUID.randomUUID());
  }

  private static String mask(String token) {
    if (token == null || token.length() < 8) {
      return "***";
    }
    return token.substring(0, 6) + "…" + token.substring(token.length() - 4);
  }
}
