package com.routeshare.notification.service;

import com.routeshare.notification.dto.NotificationPreferenceResponse;
import com.routeshare.notification.dto.NotificationResponse;
import com.routeshare.notification.dto.RegisterPushRequest;
import com.routeshare.notification.dto.UpdatePreferencesRequest;
import java.util.List;
import java.util.Map;

public interface NotificationService {
  /**
   * Persists a notification for a user and pushes it to their enabled devices (respecting prefs).
   */
  NotificationResponse deliver(
      long appUserId, String type, String title, String body, Map<String, String> data);

  List<NotificationResponse> listMine(int limit);

  List<NotificationResponse> listMine(String filter, int page, int size);

  long unreadCount();

  NotificationResponse markRead(long notificationId);

  int markAllRead();

  NotificationPreferenceResponse preferences();

  NotificationPreferenceResponse savePreferences(UpdatePreferencesRequest req);

  void registerPush(RegisterPushRequest req);

  com.routeshare.notification.dto.BadgeSummaryResponse badges();
}
