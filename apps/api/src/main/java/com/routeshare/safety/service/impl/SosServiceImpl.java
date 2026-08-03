package com.routeshare.safety.service.impl;

import com.routeshare.common.event.*;
import com.routeshare.common.ratelimit.*;
import com.routeshare.common.security.*;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.identity.provider.SmsGateway;
import com.routeshare.location.facade.LocationFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.passenger.facade.PassengerFacade;
import com.routeshare.safety.dto.*;
import com.routeshare.safety.entity.SosEventEntity;
import com.routeshare.safety.repository.SosEventRepository;
import com.routeshare.safety.service.SosService;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SosServiceImpl implements SosService {
  private static final Logger log = LoggerFactory.getLogger(SosServiceImpl.class);

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final PassengerFacade passengers;
  private final LocationFacade locations;
  private final SosEventRepository sosEvents;
  private final DomainEventPublisher events;
  private final NotificationFacade notifications;
  private final SmsGateway sms;
  private final RateLimiter rateLimiter;
  private final RateLimitProperties rateLimits;
  private final MeterRegistry meters;

  @Override
  @Transactional
  public SosEventResponse raiseCurrent(RaiseSosRequest request) {
    long appUserId = currentAppUserId();
    String mode = identityFacade.lastActiveMode(appUserId).orElse("PASSENGER");
    return raise("DRIVER".equals(mode) ? "DRIVER" : "PASSENGER", request);
  }

  @Override
  @Transactional
  public SosEventResponse raise(String ownerRole, RaiseSosRequest request) {
    long appUserId = currentAppUserId();
    rateLimiter.check(
        "sos", String.valueOf(appUserId), rateLimits.sosPerMinute(), Duration.ofMinutes(1));

    String role = "DRIVER".equalsIgnoreCase(ownerRole) ? "DRIVER" : "RIDER";
    SosEventRepository.SosContextRow context = contextFor(request, appUserId, role);
    Double latitude = request.latitude();
    Double longitude = request.longitude();
    if (request.tripId() != null) {
      try {
        var latest = locations.latestForTrip(request.tripId());
        if (latest.isPresent()) {
          latitude = latest.get().latitude();
          longitude = latest.get().longitude();
        }
      } catch (RuntimeException cacheUnavailable) {
        log.warn("sos_latest_location_unavailable tripId={}", request.tripId(), cacheUnavailable);
      }
    }
    String placeLabel = placeLabel(latitude, longitude);
    var saved =
        sosEvents.saveAndFlush(
            SosEventEntity.raise(
                appUserId,
                ownerRole,
                request.tripId(),
                request.bookingId(),
                latitude,
                longitude,
                request.note()));

    AlertResult alerts =
        alertTrustedContacts(
            appUserId,
            saved.getId(),
            request.kind(),
            context == null ? null : context.getVehicleRegistration(),
            context == null ? null : context.getDestinationLabel(),
            latitude,
            longitude);
    sosEvents.updateSnapshot(
        saved.getId(),
        context == null ? null : context.getVehicleRegistration(),
        latitude,
        longitude,
        placeLabel,
        role,
        context == null ? null : context.getDestinationLabel(),
        alerts.sent(),
        alerts.failed());
    saved.setVehicleRegistration(context == null ? null : context.getVehicleRegistration());
    saved.setSnapshotPlaceLabel(placeLabel);
    saved.setRole(role);
    saved.setDestinationLabel(context == null ? null : context.getDestinationLabel());
    saved.setContactsAlerted(alerts.sent());
    saved.setContactAlertFailures(alerts.failed());

    events.publish(
        DomainEvent.of(
            "safety.sos.raised",
            "sos_event",
            String.valueOf(saved.getId()),
            "{\"appUserId\":"
                + appUserId
                + ",\"role\":\""
                + role
                + "\",\"contactsAlerted\":"
                + alerts.sent()
                + "}"));
    notifications.notifyUser(
        appUserId,
        "SOS_RECEIVED",
        "Help is on the way",
        "Your SOS alert was received. Our safety team has been notified.",
        Map.of("sosEventId", String.valueOf(saved.getId()), "actionPath", "/safety/sos"));
    meters.counter("routeshare_sos_events_total", "role", role).increment();
    if (alerts.failed() > 0) {
      meters
          .counter("routeshare_sos_trusted_contact_alert_failures_total")
          .increment(alerts.failed());
      log.error(
          "sos_trusted_contact_alert_failed sosEventId={} failures={}",
          saved.getId(),
          alerts.failed());
    }
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SosEventResponse> listMine() {
    return sosEvents.findByAppUserIdOrderByIdDesc(currentAppUserId()).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public SosEventResponse getMine(long id) {
    return sosEvents
        .findByIdAndAppUserId(id, currentAppUserId())
        .map(this::toResponse)
        .orElseThrow(() -> new NoSuchElementException("SOS event not found"));
  }

  private SosEventRepository.SosContextRow contextFor(
      RaiseSosRequest request, long appUserId, String role) {
    if (request.tripId() == null) {
      return null;
    }
    var context =
        sosEvents
            .findContext(request.tripId(), request.bookingId())
            .orElseThrow(() -> new NoSuchElementException("Trip context not found"));
    if ("RIDER".equals(role)) {
      if (request.bookingId() == null
          || context.getPassengerAppUserId() == null
          || context.getPassengerAppUserId() != appUserId) {
        throw new AccessDeniedException("SOS trip does not belong to this rider");
      }
    } else if (context.getDriverAppUserId() == null || context.getDriverAppUserId() != appUserId) {
      throw new AccessDeniedException("SOS trip does not belong to this driver");
    }
    return context;
  }

  private AlertResult alertTrustedContacts(
      long appUserId,
      long sosEventId,
      String kind,
      String vehicleRegistration,
      String destination,
      Double latitude,
      Double longitude) {
    int sent = 0;
    int failed = 0;
    String link =
        latitude == null || longitude == null
            ? "Location unavailable"
            : "https://maps.google.com/?q=" + latitude + "," + longitude;
    String message =
        "ComiGo SOS"
            + (kind == null || kind.isBlank() ? "" : " (" + kind + ")")
            + ". "
            + (vehicleRegistration == null ? "" : "Vehicle " + vehicleRegistration + ". ")
            + (destination == null ? "" : "Destination " + destination + ". ")
            + link
            + ". Reference "
            + sosEventId
            + ".";
    for (var contact : passengers.findTrustedContacts(appUserId)) {
      if (!contact.autoShareSos() || contact.phone() == null || contact.phone().isBlank()) {
        continue;
      }
      try {
        sms.sendText(contact.phone(), message);
        sent++;
        meters.counter("routeshare_trusted_contact_alerts_total", "status", "SENT").increment();
      } catch (RuntimeException ex) {
        failed++;
        meters.counter("routeshare_trusted_contact_alerts_total", "status", "FAILED").increment();
        log.warn("trusted_contact_sms_failed sosEventId={}", sosEventId, ex);
      }
    }
    return new AlertResult(sent, failed);
  }

  private String placeLabel(Double latitude, Double longitude) {
    return latitude == null || longitude == null
        ? null
        : String.format(Locale.ROOT, "%.5f, %.5f", latitude, longitude);
  }

  private long currentAppUserId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }

  private SosEventResponse toResponse(SosEventEntity entity) {
    return new SosEventResponse(
        entity.getId(),
        entity.getStatus(),
        entity.getTripId(),
        entity.getBookingId(),
        entity.getLatitude(),
        entity.getLongitude(),
        entity.getRole(),
        entity.getVehicleRegistration(),
        entity.getDestinationLabel(),
        entity.getSnapshotPlaceLabel(),
        entity.getContactsAlerted(),
        entity.getContactAlertFailures(),
        entity.getNote(),
        entity.getCreatedAt());
  }

  private record AlertResult(int sent, int failed) {}
}
