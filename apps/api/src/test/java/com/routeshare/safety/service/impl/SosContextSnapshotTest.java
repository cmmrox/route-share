package com.routeshare.safety.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.common.ratelimit.*;
import com.routeshare.common.security.*;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.identity.provider.SmsGateway;
import com.routeshare.location.facade.LocationFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.passenger.facade.PassengerFacade;
import com.routeshare.safety.dto.RaiseSosRequest;
import com.routeshare.safety.entity.SosEventEntity;
import com.routeshare.safety.repository.SosEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.*;
import org.junit.jupiter.api.Test;

class SosContextSnapshotTest {
  @Test
  void snapshotsTripVehicleRoleAndLatestLocationAndAlertsConfiguredContacts() {
    CurrentUserProvider current = mock(CurrentUserProvider.class);
    IdentityFacade identity = mock(IdentityFacade.class);
    PassengerFacade passengers = mock(PassengerFacade.class);
    LocationFacade locations = mock(LocationFacade.class);
    SosEventRepository events = mock(SosEventRepository.class);
    SmsGateway sms = mock(SmsGateway.class);
    var token = new CurrentUser("driver-sub", "driver@test", null, "Driver", Set.of("DRIVER"));
    when(current.requireCurrentUser()).thenReturn(token);
    when(identity.upsertFromToken(token))
        .thenReturn(
            new AppUser(
                7L, UUID.randomUUID(), "driver-sub", "driver@test", null, "Driver", "ACTIVE"));
    var context = mock(SosEventRepository.SosContextRow.class);
    when(context.getDriverAppUserId()).thenReturn(7L);
    when(context.getVehicleRegistration()).thenReturn("CAA-1234");
    when(context.getDestinationLabel()).thenReturn("Fort");
    when(events.findContext(31L, 41L)).thenReturn(Optional.of(context));
    when(locations.latestForTrip(31L))
        .thenReturn(Optional.of(new LocationFacade.Snapshot(6.9271, 79.8612)));
    when(passengers.findTrustedContacts(7L))
        .thenReturn(
            List.of(
                new PassengerFacade.TrustedContact("One", "+94770000001", true),
                new PassengerFacade.TrustedContact("Two", "+94770000002", true)));
    when(events.saveAndFlush(any(SosEventEntity.class)))
        .thenAnswer(
            invocation -> {
              SosEventEntity entity = invocation.getArgument(0);
              entity.setId(51L);
              return entity;
            });
    var service =
        new SosServiceImpl(
            current,
            identity,
            passengers,
            locations,
            events,
            mock(DomainEventPublisher.class),
            mock(NotificationFacade.class),
            sms,
            (action, key, limit, window) -> {},
            new RateLimitProperties(true, null, null, null, null),
            new SimpleMeterRegistry());

    var response =
        service.raise("DRIVER", new RaiseSosRequest("EMERGENCY", 31L, 41L, 1.0, 2.0, "Need help"));

    assertThat(response.role()).isEqualTo("DRIVER");
    assertThat(response.vehicleRegistration()).isEqualTo("CAA-1234");
    assertThat(response.destinationLabel()).isEqualTo("Fort");
    assertThat(response.latitude()).isEqualTo(6.9271);
    assertThat(response.longitude()).isEqualTo(79.8612);
    assertThat(response.contactsAlerted()).isEqualTo(2);
    assertThat(response.contactAlertFailures()).isZero();
    verify(sms).sendText(eq("+94770000001"), contains("CAA-1234"));
    verify(sms).sendText(eq("+94770000002"), contains("CAA-1234"));
    verify(events)
        .updateSnapshot(
            eq(51L),
            eq("CAA-1234"),
            eq(6.9271),
            eq(79.8612),
            anyString(),
            eq("DRIVER"),
            eq("Fort"),
            eq(2),
            eq(0));
  }
}
