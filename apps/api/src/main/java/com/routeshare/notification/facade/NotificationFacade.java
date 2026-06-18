package com.routeshare.notification.facade;

import java.util.Map;

/**
 * Cross-module entry point for sending a notification to a user. Other modules (booking, trip,
 * payment, driver/vehicle review, SOS) call this instead of touching notification internals.
 */
public interface NotificationFacade {
  void notifyUser(long appUserId, String type, String title, String body, Map<String, String> data);
}
