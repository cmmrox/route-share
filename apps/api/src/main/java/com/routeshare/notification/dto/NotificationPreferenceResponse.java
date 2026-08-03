package com.routeshare.notification.dto;

import java.util.List;

public record NotificationPreferenceResponse(List<Category> categories) {
  public record Category(
      String key,
      String group,
      String label,
      boolean enabled,
      boolean push,
      boolean sms,
      boolean inApp,
      boolean mandatory) {}
}
