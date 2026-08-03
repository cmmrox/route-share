package com.routeshare.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.routeshare.location.facade.RealtimeChannelFacade;
import com.routeshare.notification.entity.PushRegistrationEntity;
import com.routeshare.notification.push.PushNotificationPort;
import com.routeshare.notification.repository.PushRegistrationRepository;
import com.routeshare.notification.service.RealtimeDeliveryService.DeliveryChannel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.*;
import org.junit.jupiter.api.Test;

class RealtimeDeliverySelectionTest {
  private final RealtimeChannelFacade realtime = mock(RealtimeChannelFacade.class);
  private final PushRegistrationRepository registrations = mock(PushRegistrationRepository.class);
  private final PushNotificationPort push = mock(PushNotificationPort.class);
  private final RealtimeDeliveryServiceImpl delivery =
      new RealtimeDeliveryServiceImpl(realtime, registrations, push, new SimpleMeterRegistry());

  @Test
  void foregroundChannelWinsWithoutSendingFcm() {
    when(realtime.deliver(eq(7L), eq("LIVE_OFFER"), anyMap())).thenReturn(true);
    assertThat(delivery.deliver(7, "LIVE_OFFER", "Offer", "New offer", Map.of(), true))
        .isEqualTo(DeliveryChannel.REALTIME);
    verifyNoInteractions(push);
  }

  @Test
  void backgroundTripCriticalMessageUsesFcmHighPriority() {
    var registration = mock(PushRegistrationEntity.class);
    when(registration.getToken()).thenReturn("token");
    when(registrations.findByAppUserIdAndEnabledTrue(7)).thenReturn(List.of(registration));
    when(push.send(any())).thenReturn(PushNotificationPort.PushResult.ok("message"));

    assertThat(delivery.deliver(7, "LIVE_OFFER", "Offer", "New offer", Map.of(), true))
        .isEqualTo(DeliveryChannel.FCM_HIGH_PRIORITY);
    verify(push).send(argThat(PushNotificationPort.PushMessage::highPriority));
  }
}
