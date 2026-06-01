package com.routeshare.passenger.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.passenger.repository.SavedPlaceRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class SavedPlaceServiceTest {
  private final CurrentUserProvider current = org.mockito.Mockito.mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = org.mockito.Mockito.mock(IdentityFacade.class);
  private final SavedPlaceRepository savedPlaces =
      org.mockito.Mockito.mock(SavedPlaceRepository.class);
  private final SavedPlaceServiceImpl service =
      new SavedPlaceServiceImpl(
          current,
          identityFacade,
          savedPlaces,
          org.mapstruct.factory.Mappers.getMapper(
              com.routeshare.passenger.mapper.PassengerMapper.class));

  @Test
  void deleteScopesByCurrentUserAndReportsMissingRows() {
    CurrentUser user =
        new CurrentUser("subject", "passenger@example.test", null, "Passenger", Set.of());
    AppUser appUser =
        new AppUser(
            42L,
            UUID.randomUUID(),
            "subject",
            "passenger@example.test",
            null,
            "Passenger",
            "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
    when(savedPlaces.deleteByIdAndAppUserId(99L, 42L)).thenReturn(0L);

    assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(AccessDeniedException.class);

    verify(savedPlaces).deleteByIdAndAppUserId(99L, 42L);
  }
}
