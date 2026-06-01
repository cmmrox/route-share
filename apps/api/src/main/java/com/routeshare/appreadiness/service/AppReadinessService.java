package com.routeshare.appreadiness.service;

import java.util.List;
import java.util.Map;

public interface AppReadinessService {
  Map<String, Object> appConfig();

  Map<String, Object> verificationStatus();

  Map<String, Object> create(
      String type,
      String ownerRole,
      String targetType,
      String targetId,
      Map<String, Object> payload);

  Map<String, Object> update(long id, Map<String, Object> payload);

  Map<String, Object> get(long id);

  List<Map<String, Object>> mine(String type, String ownerRole);

  List<Map<String, Object>> all(String type);

  Map<String, Object> markRead(long notificationId);

  Map<String, Object> preferences(String ownerRole);

  Map<String, Object> savePreferences(String ownerRole, Map<String, Object> payload);

  Map<String, Object> pushRegistration(String ownerRole, Map<String, Object> payload);

  Map<String, Object> earlyDropOff(long bookingId, Map<String, Object> payload);

  Map<String, Object> shareBooking(long bookingId, Map<String, Object> payload);

  Map<String, Object> payoutProfile(Map<String, Object> payload);

  Map<String, Object> payoutProfile();

  Map<String, Object> dashboard();

  List<Map<String, Object>> auditActions();
}
