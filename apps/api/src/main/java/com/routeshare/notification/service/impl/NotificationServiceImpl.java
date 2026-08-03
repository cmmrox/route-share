package com.routeshare.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.identity.provider.SmsGateway;
import com.routeshare.notification.dto.*;
import com.routeshare.notification.entity.*;
import com.routeshare.notification.push.PushNotificationPort;
import com.routeshare.notification.repository.*;
import com.routeshare.notification.service.NotificationService;
import com.routeshare.trip.facade.TripActivityFacade;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
  private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
  private static final int MAX_LIMIT = 100;

  private static final List<PreferenceDefinition> DEFINITIONS =
      List.of(
          def("BOOKING_DECISIONS", "RIDING", "Booking approved or declined", true, true, true),
          def("DRIVER_ARRIVING", "RIDING", "Driver arriving soon", true, false, true),
          def("TRIP_CHANGES", "RIDING", "Trip changes and cancellations", true, true, true),
          def("FEES_AND_DUES", "RIDING", "Fees and outstanding amounts", true, false, false),
          def("RECEIPTS", "RIDING", "Receipts", false, false, false),
          def("NEW_BOOKING_REQUESTS", "DRIVING", "New booking requests", true, true, true),
          def(
              "PASSENGER_CHANGES",
              "DRIVING",
              "Passenger cancelled or did not board",
              true,
              false,
              true),
          def("PAYOUTS_AND_PENALTIES", "DRIVING", "Payouts and penalties", true, false, false),
          def("DOCUMENT_EXPIRY", "DRIVING", "Document expiry reminders", true, true, true),
          def("SERVICE_UPDATES", "FROM_COMIGO", "Service updates in my area", true, false, false),
          def("OFFERS_AND_NEWS", "FROM_COMIGO", "Offers and news", false, false, false),
          def(
              "SAFETY_AND_EMERGENCY",
              "SAFETY",
              "Safety and trip-critical alerts",
              true,
              true,
              true));

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final TripActivityFacade trips;
  private final NotificationRepository notifications;
  private final NotificationPreferenceRepository preferences;
  private final PushRegistrationRepository pushRegistrations;
  private final NotificationDeliveryLogRepository deliveryLogs;
  private final PushNotificationPort push;
  private final SmsGateway sms;
  private final MeterRegistry meters;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public NotificationResponse deliver(
      long appUserId, String type, String title, String body, Map<String, String> data) {
    String category = categoryFor(type);
    String preferenceKey = preferenceKeyFor(type);
    NotificationPreferenceEntity pref = preferenceFor(appUserId, preferenceKey);
    boolean safety = "SAFETY".equals(category);
    boolean deferred = shouldDefer(appUserId, safety, type);
    String actionPath = data == null ? null : data.get("actionPath");
    var saved =
        notifications.save(
            NotificationEntity.create(
                appUserId, type, category, title, body, toJson(data), actionPath, deferred));

    boolean enabled = pref.isMandatory() || pref.isEnabled();
    if (!deferred && enabled && pref.isPush()) {
      deliverPush(appUserId, saved.getId(), title, body, data);
    }
    if (!deferred && enabled && pref.isSms()) {
      deliverSms(appUserId, saved.getId(), title, body);
    }
    meters
        .counter(
            "routeshare_notifications_delivered_total",
            "category",
            category,
            "channel",
            "IN_APP",
            "deferred",
            String.valueOf(deferred))
        .increment();
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<NotificationResponse> listMine(int limit) {
    return listMine("ALL", 0, limit);
  }

  @Override
  @Transactional(readOnly = true)
  public List<NotificationResponse> listMine(String filter, int page, int size) {
    int capped = Math.min(size <= 0 ? 30 : size, MAX_LIMIT);
    int safePage = Math.max(0, page);
    long appUserId = currentAppUserId();
    List<NotificationEntity> rows =
        switch (filter == null ? "ALL" : filter.toUpperCase(Locale.ROOT)) {
          case "ALL" ->
              notifications.findByAppUserIdOrderByIdDesc(
                  appUserId, PageRequest.of(safePage, capped));
          case "TRIPS" ->
              notifications.findByAppUserIdAndCategoryInOrderByIdDesc(
                  appUserId, List.of("RIDE", "DRIVE", "SAFETY"), PageRequest.of(safePage, capped));
          case "MONEY" ->
              notifications.findByAppUserIdAndCategoryInOrderByIdDesc(
                  appUserId, List.of("MONEY"), PageRequest.of(safePage, capped));
          case "ACCOUNT" ->
              notifications.findByAppUserIdAndCategoryInOrderByIdDesc(
                  appUserId, List.of("ACCOUNT", "BROADCAST"), PageRequest.of(safePage, capped));
          default ->
              throw new IllegalArgumentException("filter must be ALL, TRIPS, MONEY or ACCOUNT");
        };
    return rows.stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public long unreadCount() {
    return notifications.countByAppUserIdAndReadAtIsNull(currentAppUserId());
  }

  @Override
  @Transactional
  public NotificationResponse markRead(long notificationId) {
    var notification =
        notifications
            .findByIdAndAppUserId(notificationId, currentAppUserId())
            .orElseThrow(
                () -> new AccessDeniedException("Notification does not belong to current user"));
    if (notification.getReadAt() == null) {
      notification.setReadAt(Instant.now());
    }
    return toResponse(notification);
  }

  @Override
  @Transactional
  public int markAllRead() {
    return notifications.markAllRead(currentAppUserId(), Instant.now());
  }

  @Override
  @Transactional
  public NotificationPreferenceResponse preferences() {
    long appUserId = currentAppUserId();
    ensurePreferences(appUserId);
    return toPrefResponse(preferences.findByAppUserIdOrderById(appUserId));
  }

  @Override
  @Transactional
  public NotificationPreferenceResponse savePreferences(UpdatePreferencesRequest request) {
    long appUserId = currentAppUserId();
    ensurePreferences(appUserId);
    Set<String> seen = new HashSet<>();
    for (var update : request.categories()) {
      String key = update.key().toUpperCase(Locale.ROOT);
      if (!seen.add(key)) {
        throw new IllegalArgumentException("Each notification category may appear only once");
      }
      var pref =
          preferences
              .findByAppUserIdAndCategoryKey(appUserId, key)
              .orElseThrow(
                  () -> new IllegalArgumentException("Unknown notification category: " + key));
      if (pref.isMandatory()
          && (!update.enabled() || !update.push() || !update.sms() || !update.inApp())) {
        throw new GateConflictException(
            "SAFETY_NOTIFICATION_REQUIRED",
            "Safety and trip-critical alerts cannot be disabled.",
            "/settings/notifications");
      }
      pref.setEnabled(update.enabled());
      pref.setPush(update.push());
      pref.setSms(update.sms());
      pref.setInApp(update.inApp());
      pref.setUpdatedAt(Instant.now());
    }
    return toPrefResponse(preferences.findByAppUserIdOrderById(appUserId));
  }

  @Override
  @Transactional
  public void registerPush(RegisterPushRequest req) {
    long appUserId = currentAppUserId();
    var existing = pushRegistrations.findByToken(req.token());
    if (existing.isPresent()) {
      var reg = existing.get();
      reg.setAppUserId(appUserId);
      reg.setPlatform(req.platform());
      reg.setEnabled(true);
      reg.setLastSeenAt(Instant.now());
      return;
    }
    pushRegistrations.save(PushRegistrationEntity.create(appUserId, req.platform(), req.token()));
  }

  @Override
  @Transactional(readOnly = true)
  public BadgeSummaryResponse badges() {
    long appUserId = currentAppUserId();
    long unread = notifications.countByAppUserIdAndReadAtIsNull(appUserId);
    long tripUnread =
        notifications.countByAppUserIdAndReadAtIsNullAndCategoryIn(
            appUserId, List.of("RIDE", "DRIVE", "SAFETY"));
    long accountUnread =
        notifications.countByAppUserIdAndReadAtIsNullAndCategoryIn(appUserId, List.of("ACCOUNT"));
    return new BadgeSummaryResponse(
        false,
        (int) Math.min(Integer.MAX_VALUE, tripUnread),
        (int) Math.min(Integer.MAX_VALUE, unread),
        accountUnread > 0);
  }

  private void ensurePreferences(long appUserId) {
    Map<String, NotificationPreferenceEntity> existing = new HashMap<>();
    preferences
        .findByAppUserIdOrderById(appUserId)
        .forEach(entity -> existing.put(entity.getCategoryKey(), entity));
    List<NotificationPreferenceEntity> missing =
        DEFINITIONS.stream()
            .filter(definition -> !existing.containsKey(definition.key()))
            .map(
                definition ->
                    NotificationPreferenceEntity.defaultsFor(
                        appUserId,
                        definition.key(),
                        definition.defaultEnabled(),
                        definition.defaultPush(),
                        definition.defaultSms(),
                        definition.mandatory()))
            .toList();
    if (!missing.isEmpty()) {
      preferences.saveAll(missing);
    }
  }

  private NotificationPreferenceEntity preferenceFor(long appUserId, String key) {
    ensurePreferences(appUserId);
    return preferences.findByAppUserIdAndCategoryKey(appUserId, key).orElseThrow();
  }

  private boolean shouldDefer(long appUserId, boolean safety, String type) {
    if (safety || isTripCriticalDriverAlert(type)) {
      return false;
    }
    return identityFacade
        .lastActiveMode(appUserId)
        .filter("DRIVER"::equals)
        .map(ignored -> trips.hasActiveDriverTrip(appUserId))
        .orElse(false);
  }

  private void deliverPush(
      long appUserId, long notificationId, String title, String body, Map<String, String> data) {
    for (var registration : pushRegistrations.findByAppUserIdAndEnabledTrue(appUserId)) {
      var result =
          push.send(
              new PushNotificationPort.PushMessage(registration.getToken(), title, body, data));
      deliveryLogs.save(
          NotificationDeliveryLogEntity.of(
              notificationId,
              "PUSH",
              result.success() ? "SENT" : "FAILED",
              result.messageId(),
              result.error()));
    }
  }

  private void deliverSms(long appUserId, long notificationId, String title, String body) {
    var contact = identityFacade.findContact(appUserId);
    if (contact.isEmpty() || contact.get().phoneNumber() == null) {
      deliveryLogs.save(
          NotificationDeliveryLogEntity.of(
              notificationId, "SMS", "SKIPPED", null, "No verified phone number"));
      return;
    }
    try {
      sms.sendText(contact.get().phoneNumber(), title + ": " + body);
      deliveryLogs.save(
          NotificationDeliveryLogEntity.of(notificationId, "SMS", "SENT", null, null));
    } catch (RuntimeException ex) {
      deliveryLogs.save(
          NotificationDeliveryLogEntity.of(notificationId, "SMS", "FAILED", null, ex.getMessage()));
      log.warn("notification_sms_delivery_failed appUserId={}", appUserId, ex);
    }
  }

  private NotificationResponse toResponse(NotificationEntity entity) {
    return new NotificationResponse(
        entity.getId(),
        entity.getType(),
        entity.getCategory(),
        entity.getTitle(),
        entity.getBody(),
        entity.getDataJson(),
        entity.getActionPath(),
        entity.isDeferred(),
        entity.getReadAt() != null,
        entity.getCreatedAt());
  }

  private NotificationPreferenceResponse toPrefResponse(List<NotificationPreferenceEntity> rows) {
    Map<String, NotificationPreferenceEntity> byKey = new HashMap<>();
    rows.forEach(row -> byKey.put(row.getCategoryKey(), row));
    return new NotificationPreferenceResponse(
        DEFINITIONS.stream()
            .map(
                definition -> {
                  var entity = byKey.get(definition.key());
                  return new NotificationPreferenceResponse.Category(
                      definition.key(),
                      definition.group(),
                      definition.label(),
                      entity.isEnabled(),
                      entity.isPush(),
                      entity.isSms(),
                      entity.isInApp(),
                      entity.isMandatory());
                })
            .toList());
  }

  private long currentAppUserId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }

  private String toJson(Map<String, String> data) {
    if (data == null || data.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(data);
    } catch (Exception ex) {
      log.warn("notification_data_serialize_failed", ex);
      return null;
    }
  }

  private String categoryFor(String type) {
    if (type == null) {
      return "ACCOUNT";
    }
    if (type.equals("BROADCAST")) {
      return "BROADCAST";
    }
    if (type.startsWith("SOS_")) {
      return "SAFETY";
    }
    if (type.startsWith("PAYMENT_")
        || type.startsWith("PAYOUT_")
        || type.startsWith("PENALTY_")
        || type.startsWith("DUES_")) {
      return "MONEY";
    }
    if (type.startsWith("DRIVER_") || type.equals("BOOKING_REQUESTED")) {
      return "DRIVE";
    }
    if (type.startsWith("BOOKING_") || type.startsWith("TRIP_") || type.equals("CHAT_MESSAGE")) {
      return "RIDE";
    }
    return "ACCOUNT";
  }

  private String preferenceKeyFor(String type) {
    if (type == null) {
      return "SERVICE_UPDATES";
    }
    if (type.startsWith("SOS_")) {
      return "SAFETY_AND_EMERGENCY";
    }
    if (type.equals("BOOKING_REQUESTED")) {
      return "NEW_BOOKING_REQUESTS";
    }
    if (type.startsWith("BOOKING_")) {
      return "BOOKING_DECISIONS";
    }
    if (type.equals("DRIVER_ARRIVED")) {
      return "DRIVER_ARRIVING";
    }
    if (type.startsWith("PAYMENT_") || type.startsWith("DUES_")) {
      return "FEES_AND_DUES";
    }
    if (type.startsWith("PAYOUT_") || type.startsWith("PENALTY_")) {
      return "PAYOUTS_AND_PENALTIES";
    }
    if (type.equals("BROADCAST")) {
      return "SERVICE_UPDATES";
    }
    return "TRIP_CHANGES";
  }

  private boolean isTripCriticalDriverAlert(String type) {
    return type != null
        && (type.equals("BOOKING_REQUESTED")
            || type.equals("TRIP_CANCELLED")
            || type.equals("PASSENGER_NO_SHOW"));
  }

  private static PreferenceDefinition def(
      String key,
      String group,
      String label,
      boolean defaultPush,
      boolean defaultSms,
      boolean mandatory) {
    return new PreferenceDefinition(
        key,
        group,
        label,
        defaultPush || defaultSms || mandatory,
        defaultPush,
        defaultSms,
        mandatory);
  }

  private record PreferenceDefinition(
      String key,
      String group,
      String label,
      boolean defaultEnabled,
      boolean defaultPush,
      boolean defaultSms,
      boolean mandatory) {}
}
