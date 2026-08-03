package com.routeshare.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.config.TripShareProperties;
import com.routeshare.booking.dto.request.ShareTripRequest;
import com.routeshare.booking.entity.TripShareEntity;
import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.repository.TripShareRepository;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.identity.provider.SmsGateway;
import com.routeshare.passenger.facade.PassengerFacade;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class TripShareServiceImplTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final BookingRepository bookings = mock(BookingRepository.class);
  private final TripShareRepository tripShares = mock(TripShareRepository.class);
  private final PassengerFacade passengerFacade = mock(PassengerFacade.class);
  private final SmsGateway smsGateway = mock(SmsGateway.class);
  private final TripShareProperties properties =
      new TripShareProperties("https://app.routeshare.lk/share/trip", 240, 1440);
  private final TripShareServiceImpl service =
      new TripShareServiceImpl(
          current, identityFacade, bookings, tripShares, passengerFacade, smsGateway, properties);

  @BeforeEach
  void setUp() {
    var user = new CurrentUser("sub", "p@test", null, "P", Set.of("PASSENGER"));
    var appUser = new AppUser(5L, UUID.randomUUID(), "sub", "p@test", null, "P", "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
  }

  @Test
  void createsTokenizedShareForOwnedBooking() {
    when(bookings.findFareEstimateByIdAndPassengerAppUserId(9L, 5L))
        .thenReturn(Optional.of(new BigDecimal("500.00")));

    var res = service.share(9L, new ShareTripRequest(null, false));

    assertThat(res.token()).isNotBlank();
    assertThat(res.shareUrl()).startsWith("https://app.routeshare.lk/share/trip/");
    assertThat(res.contactsNotified()).isZero();
    verify(tripShares).save(any(TripShareEntity.class));
    verify(smsGateway, never()).sendText(anyString(), anyString());
  }

  @Test
  void notifiesTrustedContactsBestEffort() {
    when(bookings.findFareEstimateByIdAndPassengerAppUserId(9L, 5L))
        .thenReturn(Optional.of(new BigDecimal("500.00")));
    when(passengerFacade.findTrustedContacts(5L))
        .thenReturn(
            List.of(
                new PassengerFacade.TrustedContact("A", "+94771234567", true),
                new PassengerFacade.TrustedContact("B", "+94777654321", true)));
    doThrow(new IllegalStateException("sms down"))
        .when(smsGateway)
        .sendText(eq("+94777654321"), anyString());

    var res = service.share(9L, new ShareTripRequest(60, true));

    assertThat(res.contactsNotified()).isEqualTo(1);
    verify(smsGateway, times(2)).sendText(anyString(), anyString());
  }

  @Test
  void deniedWhenBookingNotOwned() {
    when(bookings.findFareEstimateByIdAndPassengerAppUserId(9L, 5L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.share(9L, new ShareTripRequest(null, false)))
        .isInstanceOf(AccessDeniedException.class);
    verify(tripShares, never()).save(any());
  }

  @Test
  void revokeFailsWhenTokenNotOwned() {
    when(tripShares.revoke("tok", 5L)).thenReturn(0);
    assertThatThrownBy(() -> service.revoke(9L, "tok")).isInstanceOf(AccessDeniedException.class);
  }
}
