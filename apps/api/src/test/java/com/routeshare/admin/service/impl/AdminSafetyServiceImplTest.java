package com.routeshare.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.safety.entity.SosEventEntity;
import com.routeshare.safety.repository.SosEventRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminSafetyServiceImplTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final SosEventRepository sosEvents = mock(SosEventRepository.class);
  private final NotificationFacade notifications = mock(NotificationFacade.class);
  private final AdminAuditService audit = mock(AdminAuditService.class);
  private final AdminSafetyServiceImpl service =
      new AdminSafetyServiceImpl(current, identityFacade, sosEvents, notifications, audit);

  @Test
  void resolveSetsStatusRecordsAuditAndNotifiesRaiser() {
    var admin = new CurrentUser("a", "a@test", null, "Admin", Set.of("ADMIN"));
    when(current.requireCurrentUser()).thenReturn(admin);
    when(identityFacade.upsertFromToken(admin))
        .thenReturn(new AppUser(99L, UUID.randomUUID(), "a", "a@test", null, "Admin", "ACTIVE"));
    var event = SosEventEntity.raise(7L, "PASSENGER", null, 5L, 6.9, 79.8, "unsafe");
    event.setId(3L);
    when(sosEvents.findById(3L)).thenReturn(Optional.of(event));

    var res = service.resolve(3L, "Contacted passenger, safe");

    assertThat(res.status()).isEqualTo("RESOLVED");
    assertThat(event.getStatus()).isEqualTo(SosEventEntity.RESOLVED);
    assertThat(event.getResolvedBy()).isEqualTo(99L);
    verify(audit).record(eq("SOS_RESOLVED"), eq("SOS_EVENT"), eq("3"), any());
    verify(notifications).notifyUser(eq(7L), anyString(), anyString(), anyString(), any());
  }
}
