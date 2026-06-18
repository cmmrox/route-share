package com.routeshare.notification.facade.impl;

import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.notification.service.NotificationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationFacadeImpl implements NotificationFacade {
  private final NotificationService notifications;

  @Override
  public void notifyUser(
      long appUserId, String type, String title, String body, Map<String, String> data) {
    notifications.deliver(appUserId, type, title, body, data);
  }
}
