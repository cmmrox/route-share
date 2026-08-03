package com.routeshare.passenger.service.impl;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.identity.service.PassengerIdentityProfileSyncService;
import com.routeshare.passenger.dto.request.PassengerProfileRequest;
import com.routeshare.passenger.repository.PassengerProfileRepository;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PassengerProfileServiceImplTest {
  private final CurrentUserProvider current = org.mockito.Mockito.mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = org.mockito.Mockito.mock(IdentityFacade.class);
  private final PassengerProfileRepository profiles =
      org.mockito.Mockito.mock(PassengerProfileRepository.class);
  private final PassengerIdentityProfileSyncService profileSync =
      org.mockito.Mockito.mock(PassengerIdentityProfileSyncService.class);
  private final PassengerProfileServiceImpl service =
      new PassengerProfileServiceImpl(
          current,
          identityFacade,
          profiles,
          new ObjectMapper(),
          org.mapstruct.factory.Mappers.getMapper(
              com.routeshare.passenger.mapper.PassengerMapper.class),
          profileSync,
          org.mockito.Mockito.mock(com.routeshare.rewards.facade.RewardsFacade.class));

  @Test
  void upsertSyncsSavedPassengerProfileFieldsBackToKeycloak() {
    var user = new CurrentUser("kc-user-123", null, "+94700005678", "Passenger", Set.of());
    var appUser =
        new AppUser(
            42L, UUID.randomUUID(), "kc-user-123", null, "+94700005678", "Passenger", "ACTIVE");
    var row = org.mockito.Mockito.mock(PassengerProfileRepository.PassengerProfileRow.class);
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
    when(row.id()).thenReturn(99L);
    when(row.fullName()).thenReturn("CMMROX User");
    when(row.photoUrl()).thenReturn("file:///avatar.jpg");
    when(row.preferencesJson())
        .thenReturn(
            """
        {"email":"me@example.test","referralCode":"REF1"}
        """);
    when(profiles.findByAppUserId(42L)).thenReturn(Optional.of(row));

    service.upsert(
        new PassengerProfileRequest(
            "CMMROX User",
            "file:///avatar.jpg",
            Map.of("email", "me@example.test", "referralCode", "REF1")));

    verify(profiles)
        .upsert(
            eq(42L),
            eq("CMMROX User"),
            eq("file:///avatar.jpg"),
            argThat(
                json ->
                    json.contains("\"email\":\"me@example.test\"")
                        && json.contains("\"referralCode\":\"REF1\"")));
    verify(profileSync)
        .syncPassengerProfile(
            "kc-user-123",
            "CMMROX User",
            "file:///avatar.jpg",
            Map.of("email", "me@example.test", "referralCode", "REF1"));
  }
}
