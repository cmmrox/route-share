package com.routeshare.appreadiness.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.appreadiness.entity.WorkflowItemEntity;
import com.routeshare.appreadiness.repository.WorkflowItemRepository;
import com.routeshare.appreadiness.service.AppReadinessService;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppReadinessServiceImpl implements AppReadinessService {
  private final WorkflowItemRepository items;
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final ObjectMapper objectMapper;

  public Map<String, Object> appConfig() {
    return Map.of(
        "currency", "LKR",
        "country", "LK",
        "features",
            Map.of(
                "cashPayments", true,
                "cardPayments", true,
                "liveTracking", true,
                "sos", true,
                "support", true,
                "notifications", true,
                "recurringRoutes", true),
        "support", Map.of("email", "support@routeshare.local"),
        "map", Map.of("provider", "google", "defaultZoom", 14));
  }

  public Map<String, Object> verificationStatus() {
    long appUserId = appUserId();
    return Map.of(
        "appUserId",
        appUserId,
        "driverStatus",
        "PENDING_OR_APPROVED_BY_REVIEW",
        "identityStatus",
        latestStatus("DRIVER_KYC_IDENTITY", appUserId),
        "licenceStatus",
        latestStatus("DRIVER_KYC_LICENCE", appUserId),
        "vehicleStatus",
        "CHECK_VEHICLE_LIST",
        "canCreateRoutes",
        true,
        "canOperateTrips",
        true);
  }

  @Transactional
  public Map<String, Object> create(
      String type,
      String ownerRole,
      String targetType,
      String targetId,
      Map<String, Object> payload) {
    long appUserId = appUserId();
    String title = stringValue(payload, "title", type);
    String status = stringValue(payload, "status", defaultStatus(type));
    var saved =
        items.save(
            WorkflowItemEntity.create(
                type, ownerRole, appUserId, targetType, targetId, status, title, toJson(payload)));
    audit(type + "_CREATED", targetType, targetId, Map.of("itemId", saved.getId()));
    return toMap(saved);
  }

  @Transactional
  public Map<String, Object> update(long id, Map<String, Object> payload) {
    var item = items.findById(id).orElseThrow();
    item.setStatus(stringValue(payload, "status", item.getStatus()));
    item.setTitle(stringValue(payload, "title", item.getTitle()));
    item.setPayloadJson(toJson(payload));
    return toMap(items.save(item));
  }

  public Map<String, Object> get(long id) {
    return toMap(items.findById(id).orElseThrow());
  }

  public List<Map<String, Object>> mine(String type, String ownerRole) {
    long appUserId = appUserId();
    return items.findByItemTypeAndOwnerAppUserIdOrderByIdDesc(type, appUserId).stream()
        .map(this::toMap)
        .toList();
  }

  public List<Map<String, Object>> all(String type) {
    return items.findTop50ByItemTypeOrderByIdDesc(type).stream().map(this::toMap).toList();
  }

  @Transactional
  public Map<String, Object> markRead(long notificationId) {
    var item = items.findById(notificationId).orElseThrow();
    item.setStatus("READ");
    return toMap(items.save(item));
  }

  public Map<String, Object> preferences(String ownerRole) {
    var existing = mine("NOTIFICATION_PREFERENCE", ownerRole).stream().findFirst();
    return existing.orElseGet(
        () -> Map.of("channels", Map.of("push", true, "email", true, "sms", false)));
  }

  @Transactional
  public Map<String, Object> savePreferences(String ownerRole, Map<String, Object> payload) {
    return create(
        "NOTIFICATION_PREFERENCE", ownerRole, ownerRole, String.valueOf(appUserId()), payload);
  }

  @Transactional
  public Map<String, Object> pushRegistration(String ownerRole, Map<String, Object> payload) {
    return create("PUSH_REGISTRATION", ownerRole, ownerRole, String.valueOf(appUserId()), payload);
  }

  @Transactional
  public Map<String, Object> payoutProfile(Map<String, Object> payload) {
    return create("PAYOUT_PROFILE", "DRIVER", "DRIVER", String.valueOf(appUserId()), payload);
  }

  public Map<String, Object> payoutProfile() {
    return mine("PAYOUT_PROFILE", "DRIVER").stream()
        .findFirst()
        .orElseGet(() -> Map.of("status", "NOT_CONFIGURED"));
  }

  public Map<String, Object> dashboard() {
    return Map.of(
        "generatedAt", Instant.now().toString(),
        "openSupportTickets", all("SUPPORT_TICKET").size(),
        "openSosEvents", all("SOS_EVENT").size(),
        "recentAuditActions", auditActions().stream().limit(10).toList());
  }

  public List<Map<String, Object>> auditActions() {
    return items.findTop50ByItemTypeOrderByIdDesc("AUDIT_ACTION").stream()
        .map(this::toMap)
        .toList();
  }

  private String latestStatus(String type, long appUserId) {
    return items.findByItemTypeAndOwnerAppUserIdOrderByIdDesc(type, appUserId).stream()
        .findFirst()
        .map(WorkflowItemEntity::getStatus)
        .orElse("NOT_SUBMITTED");
  }

  private void audit(
      String action, String targetType, String targetId, Map<String, Object> payload) {
    items.save(
        WorkflowItemEntity.create(
            "AUDIT_ACTION",
            "SYSTEM",
            appUserId(),
            targetType,
            targetId,
            "RECORDED",
            action,
            toJson(payload)));
  }

  private long appUserId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }

  private String defaultStatus(String type) {
    if (type.contains("SOS")) return "OPEN";
    if (type.contains("SUPPORT")) return "OPEN";
    if (type.contains("DOCUMENT") || type.contains("KYC")) return "SUBMITTED";
    return "ACTIVE";
  }

  private String stringValue(Map<String, Object> payload, String key, String fallback) {
    if (payload == null) return fallback;
    Object value = payload.get(key);
    return value == null || value.toString().isBlank() ? fallback : value.toString();
  }

  private String toJson(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Payload is not serializable");
    }
  }

  private Map<String, Object> fromJson(String json) {
    try {
      if (json == null || json.isBlank()) return Map.of();
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception ex) {
      return Map.of();
    }
  }

  private Map<String, Object> toMap(WorkflowItemEntity item) {
    var map = new LinkedHashMap<String, Object>();
    map.put("id", item.getId());
    map.put("type", item.getItemType());
    map.put("ownerRole", item.getOwnerRole());
    map.put("targetType", item.getTargetType());
    map.put("targetId", item.getTargetId());
    map.put("status", item.getStatus());
    map.put("title", item.getTitle());
    map.put("payload", fromJson(item.getPayloadJson()));
    map.put("createdAt", item.getCreatedAt());
    map.put("updatedAt", item.getUpdatedAt());
    return map;
  }
}
