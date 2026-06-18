package com.routeshare.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.dto.RegisterPushRequest;
import com.routeshare.notification.dto.UpdatePreferencesRequest;
import com.routeshare.notification.entity.NotificationEntity;
import com.routeshare.notification.entity.NotificationPreferenceEntity;
import com.routeshare.notification.entity.PushRegistrationEntity;
import com.routeshare.notification.push.PushNotificationPort;
import com.routeshare.notification.repository.NotificationDeliveryLogRepository;
import com.routeshare.notification.repository.NotificationPreferenceRepository;
import com.routeshare.notification.repository.NotificationRepository;
import com.routeshare.notification.repository.PushRegistrationRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationServiceImplTest {
  private final CurrentUserProvider current = org.mockito.Mockito.mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = org.mockito.Mockito.mock(IdentityFacade.class);
  private final NotificationRepository notifications =
      org.mockito.Mockito.mock(NotificationRepository.class);
  private final NotificationPreferenceRepository preferences =
      org.mockito.Mockito.mock(NotificationPreferenceRepository.class);
  private final PushRegistrationRepository pushRegistrations =
      org.mockito.Mockito.mock(PushRegistrationRepository.class);
  private final NotificationDeliveryLogRepository deliveryLogs =
      org.mockito.Mockito.mock(NotificationDeliveryLogRepository.class);
  private final PushNotificationPort push = org.mockito.Mockito.mock(PushNotificationPort.class);
  private final NotificationServiceImpl service =
      new NotificationServiceImpl(
          current,
          identityFacade,
          notifications,
          preferences,
          pushRegistrations,
          deliveryLogs,
          push,
          new ObjectMapper());

  @BeforeEach
  void setUp() {
    var user = new CurrentUser("sub", "u@test", null, "User", Set.of("PASSENGER"));
    var appUser = new AppUser(7L, UUID.randomUUID(), "sub", "u@test", null, "User", "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
    when(notifications.save(any(NotificationEntity.class)))
        .thenAnswer(
            inv -> {
              NotificationEntity e = inv.getArgument(0);
              e.setId(100L);
              return e;
            });
  }

  @Test
  void deliverPersistsAndPushesWhenEnabled() {
    when(preferences.findById(7L))
        .thenReturn(Optional.of(NotificationPreferenceEntity.defaultsFor(7L)));
    when(pushRegistrations.findByAppUserIdAndEnabledTrue(7L))
        .thenReturn(List.of(PushRegistrationEntity.create(7L, "ANDROID", "tok-1")));
    when(push.send(any())).thenReturn(PushNotificationPort.PushResult.ok("msg-1"));

    var res =
        service.deliver(
            7L, "BOOKING_CONFIRMED", "Booked", "Your ride is booked", Map.of("bookingId", "9"));

    assertThat(res.title()).isEqualTo("Booked");
    verify(notifications).save(any(NotificationEntity.class));
    verify(push).send(any());
    verify(deliveryLogs).save(any());
  }

  @Test
  void deliverSkipsPushWhenPushDisabled() {
    var prefs = NotificationPreferenceEntity.defaultsFor(7L);
    prefs.setPushEnabled(false);
    when(preferences.findById(7L)).thenReturn(Optional.of(prefs));

    service.deliver(7L, "MARKETING", "Hi", "Promo", null);

    verify(push, never()).send(any());
  }

  @Test
  void savePreferencesAppliesOnlyProvidedFields() {
    var prefs = NotificationPreferenceEntity.defaultsFor(7L);
    when(preferences.findById(7L)).thenReturn(Optional.of(prefs));
    when(preferences.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var res =
        service.savePreferences(new UpdatePreferencesRequest(false, null, null, null, null, true));

    assertThat(res.pushEnabled()).isFalse();
    assertThat(res.marketing()).isTrue();
    assertThat(res.tripUpdates()).isTrue(); // unchanged default
  }

  @Test
  void registerPushSavesNewToken() {
    when(pushRegistrations.findByToken("tok-new")).thenReturn(Optional.empty());

    service.registerPush(new RegisterPushRequest("IOS", "tok-new"));

    verify(pushRegistrations).save(any(PushRegistrationEntity.class));
  }
}
