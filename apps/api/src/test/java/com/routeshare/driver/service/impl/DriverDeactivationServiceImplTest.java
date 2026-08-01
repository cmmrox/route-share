package com.routeshare.driver.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.common.security.RouteShareRoles;
import com.routeshare.driver.entity.DriverDeactivationEntity;
import com.routeshare.driver.entity.DriverProfileEntity;
import com.routeshare.driver.entity.DriverReinstatementRequestEntity;
import com.routeshare.driver.repository.DriverDeactivationRepository;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.driver.repository.DriverReinstatementRequestRepository;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.support.dto.SupportTicketResponse;
import com.routeshare.support.service.SupportService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DriverDeactivationServiceImplTest {
  private static final Instant NOW = Instant.parse("2026-08-01T09:41:00Z");
  private static final long APP_USER_ID = 42L;
  private static final long PROFILE_ID = 7L;
  private static final long ADMIN_ID = 1L;

  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identity = mock(IdentityFacade.class);
  private final DriverProfileRepository drivers = mock(DriverProfileRepository.class);
  private final DriverDeactivationRepository deactivations =
      mock(DriverDeactivationRepository.class);
  private final DriverReinstatementRequestRepository requests =
      mock(DriverReinstatementRequestRepository.class);
  private final SupportService support = mock(SupportService.class);

  private final DriverDeactivationServiceImpl service =
      new DriverDeactivationServiceImpl(
          current,
          identity,
          drivers,
          deactivations,
          requests,
          support,
          Clock.fixed(NOW, ZoneOffset.UTC));

  @BeforeEach
  void setUp() {
    var token = new CurrentUser("subject-1", null, "+94771234567", "Nimali", Set.of("PASSENGER"));
    when(current.requireCurrentUser()).thenReturn(token);
    when(identity.upsertFromToken(token))
        .thenReturn(
            new AppUser(
                APP_USER_ID,
                UUID.randomUUID(),
                "subject-1",
                null,
                "+94771234567",
                "Nimali",
                "ACTIVE"));
    when(drivers.findIdByAppUserId(APP_USER_ID)).thenReturn(Optional.of(PROFILE_ID));
    when(drivers.findById(PROFILE_ID))
        .thenReturn(
            Optional.of(new DriverProfileEntity(PROFILE_ID, APP_USER_ID, "Nimali", "APPROVED")));
    when(deactivations.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(requests.save(any()))
        .thenAnswer(
            inv -> {
              DriverReinstatementRequestEntity saved = inv.getArgument(0);
              if (saved.getId() == null) {
                saved.setId(31L); // the identity column the database would assign
              }
              return saved;
            });
  }

  private DriverDeactivationEntity openDeactivation() {
    var entity =
        DriverDeactivationEntity.open(PROFILE_ID, "Three missed starts", "SL-40912", ADMIN_ID);
    entity.setId(11L);
    return entity;
  }

  @Test
  void deactivationRevokesTheDriverRole() {
    when(deactivations.findByDriverProfileIdAndReinstatedAtIsNull(PROFILE_ID))
        .thenReturn(Optional.empty());

    var result = service.deactivate(PROFILE_ID, "Three missed starts", "SL-40912", ADMIN_ID);

    assertThat(result.active()).isTrue();
    assertThat(result.caseRef()).isEqualTo("SL-40912");
    verify(identity).revokeRealmRole(APP_USER_ID, RouteShareRoles.DRIVER);
  }

  @Test
  void deactivatingTwiceKeepsTheOriginalCaseReference() {
    when(deactivations.findByDriverProfileIdAndReinstatedAtIsNull(PROFILE_ID))
        .thenReturn(Optional.of(openDeactivation()));

    var result = service.deactivate(PROFILE_ID, "Another reason", "SL-99999", ADMIN_ID);

    // The driver was already given a reference to quote; rewriting it would invalidate their
    // appeal mid-conversation.
    assertThat(result.caseRef()).isEqualTo("SL-40912");
    verify(deactivations, never()).save(any());
  }

  @Test
  void reinstatementRestoresTheRoleAndClosesTheOpenRequest() {
    var open = openDeactivation();
    when(deactivations.findByDriverProfileIdAndReinstatedAtIsNull(PROFILE_ID))
        .thenReturn(Optional.of(open));
    var request = DriverReinstatementRequestEntity.open(PROFILE_ID, 11L, "Please review", 900L);
    when(requests.findByDeactivationIdAndStatus(11L, DriverReinstatementRequestEntity.STATUS_OPEN))
        .thenReturn(Optional.of(request));

    var result = service.reinstate(PROFILE_ID, ADMIN_ID, "Appeal upheld");

    assertThat(result.active()).isFalse();
    assertThat(result.reinstatedAt()).isEqualTo(NOW);
    assertThat(request.getStatus()).isEqualTo(DriverReinstatementRequestEntity.STATUS_APPROVED);
    verify(identity).grantRealmRole(APP_USER_ID, RouteShareRoles.DRIVER);
  }

  @Test
  void reinstatingADriverWhoIsNotDeactivatedIsARefusedConflict() {
    when(deactivations.findByDriverProfileIdAndReinstatedAtIsNull(PROFILE_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.reinstate(PROFILE_ID, ADMIN_ID, null))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void aReinstatementRequestOpensASupportTicket() {
    when(deactivations.findByDriverProfileIdAndReinstatedAtIsNull(PROFILE_ID))
        .thenReturn(Optional.of(openDeactivation()));
    when(requests.findByDeactivationIdAndStatus(11L, DriverReinstatementRequestEntity.STATUS_OPEN))
        .thenReturn(Optional.empty());
    when(support.create(any(), any()))
        .thenReturn(
            new SupportTicketResponse(
                900L, "subject", "DRIVER_REINSTATEMENT", "OPEN", "HIGH", NOW, NOW, List.of()));

    var result = service.requestReinstatement("I was in hospital");

    assertThat(result.status()).isEqualTo(DriverReinstatementRequestEntity.STATUS_OPEN);
    assertThat(result.supportTicketId()).isEqualTo(900L);
  }

  @Test
  void aSecondRequestWhileOneIsOpenIsRefused() {
    when(deactivations.findByDriverProfileIdAndReinstatedAtIsNull(PROFILE_ID))
        .thenReturn(Optional.of(openDeactivation()));
    when(requests.findByDeactivationIdAndStatus(11L, DriverReinstatementRequestEntity.STATUS_OPEN))
        .thenReturn(
            Optional.of(DriverReinstatementRequestEntity.open(PROFILE_ID, 11L, "first", 900L)));

    assertThatThrownBy(() -> service.requestReinstatement("second"))
        .isInstanceOf(IllegalStateException.class);
    verify(support, never()).create(any(), any());
  }

  @Test
  void requestingReinstatementWithoutADeactivationIsRefused() {
    when(deactivations.findByDriverProfileIdAndReinstatedAtIsNull(PROFILE_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.requestReinstatement("let me back"))
        .isInstanceOf(IllegalStateException.class);
  }
}
