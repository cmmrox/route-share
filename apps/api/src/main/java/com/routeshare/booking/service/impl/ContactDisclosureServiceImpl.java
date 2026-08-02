package com.routeshare.booking.service.impl;

import com.routeshare.booking.dto.response.CounterpartyContactResponse;
import com.routeshare.booking.entity.ContactDisclosureAuditEntity;
import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.repository.ContactDisclosureAuditRepository;
import com.routeshare.booking.service.ContactDisclosureService;
import com.routeshare.common.errors.GateConflictException;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactDisclosureServiceImpl implements ContactDisclosureService {
  private static final String CONFIRMED = "CONFIRMED";
  private static final String EMERGENCY_NUMBER = "119";
  private static final String SAFETY_LINE = "1919";
  private static final int HARVEST_ALERT_THRESHOLD = 10;

  private final BookingRepository bookings;
  private final ContactDisclosureAuditRepository audits;
  private final IdentityFacade identityFacade;
  private final PolicySettingService policy;
  private final MeterRegistry meters;
  private final Clock clock;

  @Override
  @Transactional
  public CounterpartyContactResponse counterpartyFor(long bookingId, long readerAppUserId) {
    var context =
        bookings
            .findContactContext(bookingId)
            .orElseThrow(() -> new NoSuchElementException("Booking not found"));

    boolean readerIsPassenger = matches(context.getPassengerAppUserId(), readerAppUserId);
    boolean readerIsDriver = matches(context.getDriverAppUserId(), readerAppUserId);
    if (!readerIsPassenger && !readerIsDriver) {
      // Another passenger on the same trip, or a driver of a different one. Neither is a party to
      // this booking, and neither is told anything beyond that they may not look.
      throw new AccessDeniedException("This booking is not yours");
    }

    // Rule 1 and rule 5's cancellation half, in one condition: only a live confirmed booking
    // discloses anything, so decline, cancel, expiry and a no-show release all close it at once.
    if (!CONFIRMED.equalsIgnoreCase(context.getBookingStatus())) {
      throw refused(bookingId);
    }

    // Rule 4: withdrawn 24 hours after the ride ends. Drop-off is the honest clock — a trip marked
    // complete hours later would keep her number readable long after she got out.
    Instant rideEnded =
        context.getDroppedOffAt() != null ? context.getDroppedOffAt() : context.getCompletedAt();
    if (rideEnded != null) {
      Instant closesAt =
          rideEnded.plus(
              Duration.ofHours(policy.integer(PolicyKey.CONTACT_DISCLOSURE_HOURS_AFTER_DROPOFF)));
      if (clock.instant().isAfter(closesAt)) {
        throw refused(bookingId);
      }
    }

    long subjectAppUserId =
        readerIsPassenger ? context.getDriverAppUserId() : context.getPassengerAppUserId();
    var contact =
        identityFacade
            .findContact(subjectAppUserId)
            .filter(row -> row.phoneNumber() != null && !row.phoneNumber().isBlank())
            .orElseThrow(() -> refused(bookingId));

    // Audited before the number is returned, and on every read. A row written only on the first
    // read would hide exactly the pattern this table exists to expose.
    audits.save(ContactDisclosureAuditEntity.of(bookingId, readerAppUserId, subjectAppUserId));
    meters
        .counter(
            "routeshare_contact_disclosures_total",
            "role",
            readerIsPassenger ? "PASSENGER" : "DRIVER")
        .increment();

    int distinctToday =
        audits.countDistinctSubjectsSince(
            readerAppUserId, clock.instant().minus(Duration.ofDays(1)));
    if (distinctToday >= HARVEST_ALERT_THRESHOLD) {
      // Nothing about one disclosure looks wrong; the pattern is the only signal there is.
      log.warn(
          "contact disclosure volume: appUserId={} read {} distinct numbers in 24h",
          readerAppUserId,
          distinctToday);
      meters.counter("routeshare_contact_disclosure_volume_alerts_total").increment();
    }

    return new CounterpartyContactResponse(
        bookingId,
        readerIsPassenger ? "DRIVER" : "PASSENGER",
        contact.firstName() == null ? "Your driver" : contact.firstName(),
        contact.phoneNumber(),
        EMERGENCY_NUMBER,
        SAFETY_LINE);
  }

  /**
   * One refusal for every reason. Telling a caller <em>which</em> rule stopped them turns this
   * endpoint into a probe: "not yet confirmed" and "revoked yesterday" are both facts about someone
   * else's trip.
   */
  private GateConflictException refused(long bookingId) {
    return new GateConflictException(
        "CONTACT_NOT_AVAILABLE",
        "Phone numbers are shared only while a confirmed trip is under way.",
        "/passenger/bookings/" + bookingId);
  }

  private static boolean matches(Long candidate, long appUserId) {
    return candidate != null && candidate == appUserId;
  }
}
