package com.routeshare.identity.facade.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.repository.AppUserRepository;
import com.routeshare.identity.repository.AppUserStatusHistoryRepository;
import com.routeshare.identity.service.AccountRoleService;
import com.routeshare.identity.service.KeycloakRealmRoleService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IdentityFacadeImplTest {
  private static final CurrentUser TOKEN =
      new CurrentUser("subject-1", "a@b.lk", "+94711234567", "Amal", Set.of("PASSENGER"));
  private static final AppUser APP_USER =
      new AppUser(
          7L,
          java.util.UUID.fromString("00000000-0000-0000-0000-000000000007"),
          "subject-1",
          "a@b.lk",
          "+94711234567",
          "Amal",
          "ACTIVE");

  @Test
  void cachesProjectionSoRepeatRequestsSkipTheDatabaseWrite() {
    var repository = mock(AppUserRepository.class);
    when(repository.upsertFromToken(TOKEN)).thenReturn(APP_USER);
    var facade =
        new IdentityFacadeImpl(
            repository,
            mock(AppUserStatusHistoryRepository.class),
            mock(KeycloakRealmRoleService.class),
            mock(AccountRoleService.class),
            new SimpleMeterRegistry(),
            300);

    assertThat(facade.upsertFromToken(TOKEN)).isEqualTo(APP_USER);
    assertThat(facade.upsertFromToken(TOKEN)).isEqualTo(APP_USER);
    assertThat(facade.upsertFromToken(TOKEN)).isEqualTo(APP_USER);

    verify(repository, times(1)).upsertFromToken(TOKEN);
  }

  @Test
  void changedTokenClaimsBypassTheCacheAndReSyncTheProjection() {
    var repository = mock(AppUserRepository.class);
    var changedToken =
        new CurrentUser("subject-1", "new@b.lk", "+94711234567", "Amal", Set.of("PASSENGER"));
    var changedUser =
        new AppUser(
            7L,
            java.util.UUID.fromString("00000000-0000-0000-0000-000000000007"),
            "subject-1",
            "new@b.lk",
            "+94711234567",
            "Amal",
            "ACTIVE");
    when(repository.upsertFromToken(TOKEN)).thenReturn(APP_USER);
    when(repository.upsertFromToken(changedToken)).thenReturn(changedUser);
    var facade =
        new IdentityFacadeImpl(
            repository,
            mock(AppUserStatusHistoryRepository.class),
            mock(KeycloakRealmRoleService.class),
            mock(AccountRoleService.class),
            new SimpleMeterRegistry(),
            300);

    facade.upsertFromToken(TOKEN);
    assertThat(facade.upsertFromToken(changedToken)).isEqualTo(changedUser);

    verify(repository, times(1)).upsertFromToken(TOKEN);
    verify(repository, times(1)).upsertFromToken(changedToken);
  }

  @Test
  void invalidationForcesTheNextRequestBackToTheDatabase() {
    var repository = mock(AppUserRepository.class);
    when(repository.upsertFromToken(TOKEN)).thenReturn(APP_USER);
    var facade =
        new IdentityFacadeImpl(
            repository,
            mock(AppUserStatusHistoryRepository.class),
            mock(KeycloakRealmRoleService.class),
            mock(AccountRoleService.class),
            new SimpleMeterRegistry(),
            300);

    facade.upsertFromToken(TOKEN);
    facade.invalidateProjection("subject-1");
    facade.upsertFromToken(TOKEN);

    verify(repository, times(2)).upsertFromToken(TOKEN);
  }

  @Test
  void zeroTtlDisablesCachingEntirely() {
    var repository = mock(AppUserRepository.class);
    when(repository.upsertFromToken(TOKEN)).thenReturn(APP_USER);
    var facade =
        new IdentityFacadeImpl(
            repository,
            mock(AppUserStatusHistoryRepository.class),
            mock(KeycloakRealmRoleService.class),
            mock(AccountRoleService.class),
            new SimpleMeterRegistry(),
            0);

    facade.upsertFromToken(TOKEN);
    facade.upsertFromToken(TOKEN);

    verify(repository, times(2)).upsertFromToken(TOKEN);
  }
}
