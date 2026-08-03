package com.routeshare.notification.service.impl;

import com.routeshare.location.facade.RealtimeChannelFacade;
import com.routeshare.notification.push.PushNotificationPort;
import com.routeshare.notification.repository.PushRegistrationRepository;
import com.routeshare.notification.service.RealtimeDeliveryService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealtimeDeliveryServiceImpl implements RealtimeDeliveryService {
  private final RealtimeChannelFacade realtime;
  private final PushRegistrationRepository registrations;
  private final PushNotificationPort push;
  private final MeterRegistry meters;

  @Override
  public DeliveryChannel deliver(
      long appUserId,
      String eventType,
      String title,
      String body,
      Map<String, String> data,
      boolean tripCritical) {
    if (realtime.deliver(appUserId, eventType, data)) {
      count("REALTIME");
      return DeliveryChannel.REALTIME;
    }
    boolean highPriority = tripCritical || eventType.startsWith("SOS_");
    boolean sent = false;
    for (var registration : registrations.findByAppUserIdAndEnabledTrue(appUserId)) {
      sent |=
          push.send(
                  new PushNotificationPort.PushMessage(
                      registration.getToken(), title, body, data, highPriority))
              .success();
    }
    DeliveryChannel channel =
        !sent
            ? DeliveryChannel.UNDELIVERABLE
            : highPriority
                ? DeliveryChannel.FCM_HIGH_PRIORITY
                : DeliveryChannel.FCM_NORMAL_PRIORITY;
    count(channel.name());
    return channel;
  }

  private void count(String channel) {
    meters.counter("routeshare_realtime_delivery_total", "channel", channel).increment();
  }
}
