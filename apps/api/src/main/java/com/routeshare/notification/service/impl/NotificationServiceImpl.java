package com.routeshare.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.dto.NotificationPreferenceResponse;
import com.routeshare.notification.dto.NotificationResponse;
import com.routeshare.notification.dto.RegisterPushRequest;
import com.routeshare.notification.dto.UpdatePreferencesRequest;
import com.routeshare.notification.entity.NotificationDeliveryLogEntity;
import com.routeshare.notification.entity.NotificationEntity;
import com.routeshare.notification.entity.NotificationPreferenceEntity;
import com.routeshare.notification.push.PushNotificationPort;
import com.routeshare.notification.repository.NotificationDeliveryLogRepository;
import com.routeshare.notification.repository.NotificationPreferenceRepository;
import com.routeshare.notification.repository.NotificationRepository;
import com.routeshare.notification.repository.PushRegistrationRepository;
import com.routeshare.notification.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
  private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
  private static final int MAX_LIMIT = 100;

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final NotificationRepository notifications;
  private final NotificationPreferenceRepository preferences;
  private final PushRegistrationRepository pushRegistrations;
  private final NotificationDeliveryLogRepository deliveryLogs;
  private final PushNotificationPort push;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public NotificationResponse deliver(
      long appUserId, String type, String title, String body, Map<String, String> data) {
    var saved =
        notifications.save(NotificationEntity.create(appUserId, type, title, body, toJson(data)));
    var prefs = prefsFor(appUserId);
    if (prefs.isPushEnabled()) {
      for (var reg : pushRegistrations.findByAppUserIdAndEnabledTrue(appUserId)) {
        var result =
            push.send(new PushNotificationPort.PushMessage(reg.getToken(), title, body, data));
        deliveryLogs.save(
            NotificationDeliveryLogEntity.of(
                saved.getId(),
                "PUSH",
                result.success() ? "SENT" : "FAILED",
                result.messageId(),
                result.error()));
      }
    }
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<NotificationResponse> listMine(int limit) {
    int capped = Math.min(limit <= 0 ? 30 : limit, MAX_LIMIT);
    return notifications
        .findByAppUserIdOrderByIdDesc(currentAppUserId(), PageRequest.of(0, capped))
        .stream()
        .map(this::toResponse)
        .toList();
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
  public NotificationPreferenceResponse preferences() {
    return toPrefResponse(prefsFor(currentAppUserId()));
  }

  @Override
  @Transactional
  public NotificationPreferenceResponse savePreferences(UpdatePreferencesRequest req) {
    var prefs = prefsFor(currentAppUserId());
    if (req.pushEnabled() != null) {
      prefs.setPushEnabled(req.pushEnabled());
    }
    if (req.emailEnabled() != null) {
      prefs.setEmailEnabled(req.emailEnabled());
    }
    if (req.bookingUpdates() != null) {
      prefs.setBookingUpdates(req.bookingUpdates());
    }
    if (req.tripUpdates() != null) {
      prefs.setTripUpdates(req.tripUpdates());
    }
    if (req.paymentUpdates() != null) {
      prefs.setPaymentUpdates(req.paymentUpdates());
    }
    if (req.marketing() != null) {
      prefs.setMarketing(req.marketing());
    }
    prefs.setUpdatedAt(Instant.now());
    return toPrefResponse(preferences.save(prefs));
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
    pushRegistrations.save(
        com.routeshare.notification.entity.PushRegistrationEntity.create(
            appUserId, req.platform(), req.token()));
  }

  private NotificationPreferenceEntity prefsFor(long appUserId) {
    return preferences
        .findById(appUserId)
        .orElseGet(() -> preferences.save(NotificationPreferenceEntity.defaultsFor(appUserId)));
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
    } catch (Exception e) {
      log.warn("notification_data_serialize_failed", e);
      return null;
    }
  }

  private NotificationResponse toResponse(NotificationEntity e) {
    return new NotificationResponse(
        e.getId(),
        e.getType(),
        e.getTitle(),
        e.getBody(),
        e.getDataJson(),
        e.getReadAt() != null,
        e.getCreatedAt());
  }

  private NotificationPreferenceResponse toPrefResponse(NotificationPreferenceEntity e) {
    return new NotificationPreferenceResponse(
        e.isPushEnabled(),
        e.isEmailEnabled(),
        e.isBookingUpdates(),
        e.isTripUpdates(),
        e.isPaymentUpdates(),
        e.isMarketing());
  }
}
