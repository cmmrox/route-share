package com.routeshare.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.repository.ContactDisclosureAuditRepository;
import com.routeshare.common.errors.GateConflictException;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * Plan §6.1, every negative path.
 *
 * <p>This is the one place in the product where a mistake cannot be undone: a phone number
 * disclosed once is held by the other person for ever, after the ride, after the account is deleted
 * and after any apology. So the refusals are tested more thoroughly than the success — and the
 * success is tested for having written its audit row, because that trail is what a harassment
 * report is investigated from.
 */
class ContactDisclosureAuthorizationTest {
  private static final long BOOKING = 42L;
  private static final long PASSENGER = 100L;
  private static final long DRIVER = 200L;
  private static final long STRANGER = 300L;
  private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");

  private final BookingRepository bookings = mock(BookingRepository.class);
  private final ContactDisclosureAuditRepository audits =
      mock(ContactDisclosureAuditRepository.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final PolicySettingService policy = mock(PolicySettingService.class);

  private ContactDisclosureServiceImpl service() {
    when(policy.integer(PolicyKey.CONTACT_DISCLOSURE_HOURS_AFTER_DROPOFF)).thenReturn(24);
    when(identityFacade.findContact(DRIVER))
        .thenReturn(Optional.of(new IdentityFacade.Contact(DRIVER, "Priya", "+94771234567")));
    when(identityFacade.findContact(PASSENGER))
        .thenReturn(Optional.of(new IdentityFacade.Contact(PASSENGER, "Dinuka", "+94777654321")));
    return new ContactDisclosureServiceImpl(
        bookings,
        audits,
        identityFacade,
        policy,
        new SimpleMeterRegistry(),
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private void context(String status, Instant droppedOffAt) {
    var row = mock(BookingRepository.ContactContextRow.class);
    when(row.getBookingId()).thenReturn(BOOKING);
    when(row.getBookingStatus()).thenReturn(status);
    when(row.getPassengerAppUserId()).thenReturn(PASSENGER);
    when(row.getDriverAppUserId()).thenReturn(DRIVER);
    when(row.getDroppedOffAt()).thenReturn(droppedOffAt);
    when(row.getCompletedAt()).thenReturn(droppedOffAt);
    when(bookings.findContactContext(BOOKING)).thenReturn(Optional.of(row));
  }

  @Test
  @DisplayName("07-16: a confirmed booking discloses both ways, and both reads are audited")
  void confirmedBookingDisclosesReciprocally() {
    context("CONFIRMED", null);
    var service = service();

    var toPassenger = service.counterpartyFor(BOOKING, PASSENGER);
    assertThat(toPassenger.role()).isEqualTo("DRIVER");
    assertThat(toPassenger.phoneNumber()).isEqualTo("+94771234567");
    assertThat(toPassenger.firstName()).isEqualTo("Priya");

    var toDriver = service.counterpartyFor(BOOKING, DRIVER);
    assertThat(toDriver.role()).isEqualTo("PASSENGER");
    assertThat(toDriver.phoneNumber()).isEqualTo("+94777654321");

    verify(audits, times(2)).save(any());
  }

  @Test
  @DisplayName("Every read is audited, including a repeat — the pattern is the whole point")
  void repeatedReadsAreAuditedEveryTime() {
    context("CONFIRMED", null);
    var service = service();

    service.counterpartyFor(BOOKING, PASSENGER);
    service.counterpartyFor(BOOKING, PASSENGER);
    service.counterpartyFor(BOOKING, PASSENGER);

    verify(audits, times(3)).save(any());
  }

  @Test
  @DisplayName("07-15: a request the driver has not accepted discloses nothing")
  void pendingRequestIsRefused() {
    context("REQUESTED", null);
    assertRefused(PASSENGER);
  }

  @Test
  @DisplayName("07-18: a cancelled booking discloses nothing")
  void cancelledIsRefused() {
    context("CANCELLED", null);
    assertRefused(PASSENGER);
  }

  @Test
  @DisplayName("A declined booking discloses nothing")
  void declinedIsRefused() {
    context("REJECTED", null);
    assertRefused(DRIVER);
  }

  @Test
  @DisplayName("An expired request discloses nothing")
  void expiredIsRefused() {
    context("EXPIRED", null);
    assertRefused(PASSENGER);
  }

  @Test
  @DisplayName("07-17: 25 hours after drop-off the number is withdrawn")
  void withdrawnADayAfterTheRide() {
    context("CONFIRMED", NOW.minus(Duration.ofHours(25)));
    assertRefused(PASSENGER);
  }

  @Test
  @DisplayName("23 hours after drop-off it is still readable")
  void stillReadableInsideTheWindow() {
    context("CONFIRMED", NOW.minus(Duration.ofHours(23)));
    assertThat(service().counterpartyFor(BOOKING, PASSENGER).phoneNumber()).isNotBlank();
  }

  @Test
  @DisplayName("07-19 / 07-20: anyone who is not a party to this booking is refused outright")
  void strangersAreRefused() {
    context("CONFIRMED", null);
    // Another passenger on the same trip, and a driver running a different one, reach here the
    // same way and are told the same thing.
    assertThatThrownBy(() -> service().counterpartyFor(BOOKING, STRANGER))
        .isInstanceOf(AccessDeniedException.class);
    verify(audits, never()).save(any());
  }

  @Test
  @DisplayName("A counterparty with no number on file is a refusal, not an empty string")
  void missingNumberIsRefused() {
    context("CONFIRMED", null);
    // Built before the stub is narrowed, because service() seeds the happy-path contacts.
    var service = service();
    when(identityFacade.findContact(DRIVER))
        .thenReturn(Optional.of(new IdentityFacade.Contact(DRIVER, "Priya", null)));

    assertThatThrownBy(() -> service.counterpartyFor(BOOKING, PASSENGER))
        .isInstanceOf(GateConflictException.class)
        .hasMessageContaining("confirmed trip");
    verify(audits, never()).save(any());
  }

  @Test
  @DisplayName("Every refusal carries the same code — a precise reason is itself a probe")
  void refusalsAreIndistinguishable() {
    context("REQUESTED", null);
    var pending = catchGate(PASSENGER);
    context("CONFIRMED", NOW.minus(Duration.ofHours(48)));
    var expired = catchGate(PASSENGER);

    assertThat(pending.code()).isEqualTo("CONTACT_NOT_AVAILABLE");
    assertThat(expired.code()).isEqualTo("CONTACT_NOT_AVAILABLE");
    assertThat(pending.getMessage()).isEqualTo(expired.getMessage());
  }

  @Test
  @DisplayName("The emergency numbers ride along and are never subject to these rules")
  void emergencyNumbersAlwaysTravel() {
    context("CONFIRMED", null);
    var response = service().counterpartyFor(BOOKING, PASSENGER);
    assertThat(response.emergencyNumber()).isEqualTo("119");
    assertThat(response.safetyLineNumber()).isNotBlank();
  }

  private void assertRefused(long readerAppUserId) {
    assertThatThrownBy(() -> service().counterpartyFor(BOOKING, readerAppUserId))
        .isInstanceOf(GateConflictException.class)
        .hasMessageContaining("confirmed trip");
    verify(audits, never()).save(any());
    verify(audits, never()).countDistinctSubjectsSince(anyLong(), any());
  }

  private GateConflictException catchGate(long readerAppUserId) {
    try {
      service().counterpartyFor(BOOKING, readerAppUserId);
      throw new AssertionError("expected a refusal");
    } catch (GateConflictException expected) {
      return expected;
    }
  }
}
