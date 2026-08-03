package com.routeshare.routing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.routing.entity.RouteOccurrenceShareEntity;
import com.routeshare.routing.repository.RouteOccurrenceRepository;
import com.routeshare.routing.repository.RouteOccurrenceShareRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class TripShareCodeServiceImplTest {
  private static final long OCCURRENCE_ID = 41L;
  private static final long APP_USER_ID = 7L;

  private final RouteOccurrenceShareRepository shares =
      org.mockito.Mockito.mock(RouteOccurrenceShareRepository.class);
  private final RouteOccurrenceRepository occurrences =
      org.mockito.Mockito.mock(RouteOccurrenceRepository.class);
  private final CurrentUserProvider current = org.mockito.Mockito.mock(CurrentUserProvider.class);
  private final IdentityFacade identity = org.mockito.Mockito.mock(IdentityFacade.class);
  private final CurrentUser token =
      new CurrentUser("driver-sub", null, null, "Driver", Set.of("DRIVER"));
  private final TripShareCodeServiceImpl service =
      new TripShareCodeServiceImpl(
          shares,
          occurrences,
          current,
          identity,
          Clock.fixed(Instant.parse("2026-08-03T10:00:00Z"), ZoneOffset.UTC),
          "https://comigo.test/r/");

  @BeforeEach
  void setUpIdentity() {
    when(current.requireCurrentUser()).thenReturn(token);
    when(identity.upsertFromToken(token))
        .thenReturn(
            new AppUser(
                APP_USER_ID, UUID.randomUUID(), "driver-sub", null, null, "Driver", "ACTIVE"));
  }

  @Test
  void ownerCanReadExistingShareWithoutMintingANewCode() {
    var share = RouteOccurrenceShareEntity.of(OCCURRENCE_ID, "0123456789");
    when(occurrences.isOwnedByDriverAppUser(OCCURRENCE_ID, APP_USER_ID)).thenReturn(true);
    when(shares.findByRouteOccurrenceId(OCCURRENCE_ID)).thenReturn(Optional.of(share));

    var response = service.getFor(OCCURRENCE_ID);

    assertThat(response.shortCode()).isEqualTo("0123456789");
    assertThat(response.shortUrl()).isEqualTo("https://comigo.test/r/0123456789");
    verify(shares, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void driverCannotReadOrRotateAnotherDriversShare() {
    when(occurrences.isOwnedByDriverAppUser(OCCURRENCE_ID, APP_USER_ID)).thenReturn(false);

    assertThatThrownBy(() -> service.getFor(OCCURRENCE_ID))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> service.shareFor(OCCURRENCE_ID))
        .isInstanceOf(AccessDeniedException.class);
    verify(shares, never()).findByRouteOccurrenceId(OCCURRENCE_ID);
  }
}
