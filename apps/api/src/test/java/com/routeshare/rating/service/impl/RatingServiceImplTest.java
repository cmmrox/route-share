package com.routeshare.rating.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.facade.BookingFacade;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.rating.dto.RateBookingRequest;
import com.routeshare.rating.entity.RatingEntity;
import com.routeshare.rating.repository.RatingRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class RatingServiceImplTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final BookingFacade bookingFacade = mock(BookingFacade.class);
  private final RatingRepository ratings = mock(RatingRepository.class);
  private final NotificationFacade notifications = mock(NotificationFacade.class);
  private final RatingServiceImpl service =
      new RatingServiceImpl(current, identityFacade, bookingFacade, ratings, notifications);

  @BeforeEach
  void setUp() {
    var user = new CurrentUser("sub", "p@test", null, "P", Set.of("PASSENGER"));
    var appUser = new AppUser(5L, UUID.randomUUID(), "sub", "p@test", null, "P", "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
  }

  @Test
  void rateResolvesDriverPersistsAndNotifies() {
    when(bookingFacade.findDriverAppUserIdForPassengerBooking(9L, 5L)).thenReturn(Optional.of(42L));
    when(ratings.existsByBookingIdAndRaterAppUserId(9L, 5L)).thenReturn(false);
    when(ratings.save(any(RatingEntity.class)))
        .thenAnswer(
            inv -> {
              RatingEntity e = inv.getArgument(0);
              e.setId(55L);
              return e;
            });

    var res = service.ratePassengerBooking(9L, new RateBookingRequest(5, "Great ride"));

    assertThat(res.stars()).isEqualTo(5);
    verify(ratings).save(any(RatingEntity.class));
    verify(notifications)
        .notifyUser(
            eqLong(42L), org.mockito.ArgumentMatchers.eq("RATING_RECEIVED"), any(), any(), any());
  }

  @Test
  void rateDeniedWhenBookingNotOwned() {
    when(bookingFacade.findDriverAppUserIdForPassengerBooking(9L, 5L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.ratePassengerBooking(9L, new RateBookingRequest(4, null)))
        .isInstanceOf(AccessDeniedException.class);
    verify(ratings, never()).save(any());
  }

  @Test
  void rateRejectsDuplicate() {
    when(bookingFacade.findDriverAppUserIdForPassengerBooking(9L, 5L)).thenReturn(Optional.of(42L));
    when(ratings.existsByBookingIdAndRaterAppUserId(9L, 5L)).thenReturn(true);
    assertThatThrownBy(() -> service.ratePassengerBooking(9L, new RateBookingRequest(4, null)))
        .isInstanceOf(IllegalStateException.class);
    verify(ratings, never()).save(any());
  }

  private static long eqLong(long v) {
    return org.mockito.ArgumentMatchers.eq(v);
  }
}
