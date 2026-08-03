package com.routeshare.notification.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.security.*;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.identity.provider.SmsGateway;
import com.routeshare.notification.dto.*;
import com.routeshare.notification.entity.*;
import com.routeshare.notification.push.PushNotificationPort;
import com.routeshare.notification.repository.*;
import com.routeshare.trip.facade.TripActivityFacade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.*;
import org.junit.jupiter.api.*;

class NotificationServiceImplTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identity = mock(IdentityFacade.class);
  private final TripActivityFacade trips = mock(TripActivityFacade.class);
  private final NotificationRepository notifications = mock(NotificationRepository.class);
  private final NotificationPreferenceRepository preferences =
      mock(NotificationPreferenceRepository.class);
  private final PushRegistrationRepository registrations = mock(PushRegistrationRepository.class);
  private final NotificationDeliveryLogRepository logs =
      mock(NotificationDeliveryLogRepository.class);
  private final PushNotificationPort push = mock(PushNotificationPort.class);
  private final SmsGateway sms = mock(SmsGateway.class);
  private final NotificationServiceImpl service =
      new NotificationServiceImpl(
          current,
          identity,
          trips,
          notifications,
          preferences,
          registrations,
          logs,
          push,
          sms,
          new SimpleMeterRegistry(),
          new ObjectMapper());

  @BeforeEach
  void setUp() {
    var token = new CurrentUser("sub", "u@test", "+94770000000", "User", Set.of("PASSENGER"));
    when(current.requireCurrentUser()).thenReturn(token);
    when(identity.upsertFromToken(token))
        .thenReturn(new AppUser(7L, UUID.randomUUID(), "sub", "u@test", null, "User", "ACTIVE"));
    when(notifications.save(any(NotificationEntity.class)))
        .thenAnswer(
            invocation -> {
              NotificationEntity entity = invocation.getArgument(0);
              entity.setId(100L);
              return entity;
            });
    when(preferences.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void deliversTripCriticalNotificationToPushAndSms() {
    var pref =
        NotificationPreferenceEntity.defaultsFor(7L, "BOOKING_DECISIONS", true, true, true, true);
    when(preferences.findByAppUserIdOrderById(7L)).thenReturn(List.of(pref));
    when(preferences.findByAppUserIdAndCategoryKey(7L, "BOOKING_DECISIONS"))
        .thenReturn(Optional.of(pref));
    when(registrations.findByAppUserIdAndEnabledTrue(7L))
        .thenReturn(List.of(PushRegistrationEntity.create(7L, "ANDROID", "tok")));
    when(push.send(any())).thenReturn(PushNotificationPort.PushResult.ok("message"));
    when(identity.findContact(7L))
        .thenReturn(Optional.of(new IdentityFacade.Contact(7L, "User", "+94770000000")));

    var response =
        service.deliver(
            7L, "BOOKING_CONFIRMED", "Booked", "Your seat is confirmed", Map.of("bookingId", "9"));

    assertThat(response.category()).isEqualTo("RIDE");
    verify(push).send(any());
    verify(sms).sendText("+94770000000", "Booked: Your seat is confirmed");
  }

  @Test
  void passengerAlertIsStoredDeferredDuringLiveDriverMode() {
    var pref =
        NotificationPreferenceEntity.defaultsFor(7L, "TRIP_CHANGES", true, true, false, false);
    when(preferences.findByAppUserIdOrderById(7L)).thenReturn(List.of(pref));
    when(preferences.findByAppUserIdAndCategoryKey(7L, "TRIP_CHANGES"))
        .thenReturn(Optional.of(pref));
    when(identity.lastActiveMode(7L)).thenReturn(Optional.of("DRIVER"));
    when(trips.hasActiveDriverTrip(7L)).thenReturn(true);

    var response = service.deliver(7L, "CHAT_MESSAGE", "New message", "Open chat", null);

    assertThat(response.deferred()).isTrue();
    verify(push, never()).send(any());
    verify(sms, never()).sendText(any(), any());
  }

  @Test
  void refusesToDisableSafetyCategory() {
    var safety =
        NotificationPreferenceEntity.defaultsFor(
            7L, "SAFETY_AND_EMERGENCY", true, true, true, true);
    when(preferences.findByAppUserIdOrderById(7L)).thenReturn(List.of(safety));
    when(preferences.findByAppUserIdAndCategoryKey(7L, "SAFETY_AND_EMERGENCY"))
        .thenReturn(Optional.of(safety));

    var update =
        new UpdatePreferencesRequest(
            List.of(
                new UpdatePreferencesRequest.Category(
                    "SAFETY_AND_EMERGENCY", false, false, false, false)));

    assertThatThrownBy(() -> service.savePreferences(update))
        .isInstanceOf(GateConflictException.class)
        .hasMessageContaining("cannot be disabled");
  }

  @Test
  void returnsAllTwelvePreferenceRowsWithThreeChannels() {
    when(preferences.findByAppUserIdOrderById(7L)).thenReturn(List.of());
    when(preferences.saveAll(any()))
        .thenAnswer(
            invocation -> {
              List<NotificationPreferenceEntity> seeded = invocation.getArgument(0);
              when(preferences.findByAppUserIdOrderById(7L)).thenReturn(seeded);
              return seeded;
            });

    var response = service.preferences();

    assertThat(response.categories()).hasSize(12);
    assertThat(response.categories())
        .allSatisfy(
            category -> {
              assertThat(category.key()).isNotBlank();
              assertThat(category.group()).isNotBlank();
              assertThat(category.label()).isNotBlank();
            });
  }

  @Test
  void badgePayloadUsesDotsForHomeAndAccountAndCountsForTripsAndInbox() {
    when(notifications.countByAppUserIdAndReadAtIsNull(7L)).thenReturn(9L);
    when(notifications.countByAppUserIdAndReadAtIsNullAndCategoryIn(
            7L, List.of("RIDE", "DRIVE", "SAFETY")))
        .thenReturn(4L);
    when(notifications.countByAppUserIdAndReadAtIsNullAndCategoryIn(7L, List.of("ACCOUNT")))
        .thenReturn(1L);

    var badges = service.badges();

    assertThat(badges.home()).isFalse();
    assertThat(badges.trips()).isEqualTo(4);
    assertThat(badges.inbox()).isEqualTo(9);
    assertThat(badges.account()).isTrue();
  }
}
