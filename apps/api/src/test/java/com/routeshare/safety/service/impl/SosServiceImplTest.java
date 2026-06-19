package com.routeshare.safety.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.safety.dto.RaiseSosRequest;
import com.routeshare.safety.entity.SosEventEntity;
import com.routeshare.safety.repository.SosEventRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SosServiceImplTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final SosEventRepository sosEvents = mock(SosEventRepository.class);
  private final DomainEventPublisher events = mock(DomainEventPublisher.class);
  private final NotificationFacade notifications = mock(NotificationFacade.class);
  private final SosServiceImpl service =
      new SosServiceImpl(current, identityFacade, sosEvents, events, notifications);

  @BeforeEach
  void setUp() {
    var user = new CurrentUser("sub", "p@test", null, "P", Set.of("PASSENGER"));
    var appUser = new AppUser(5L, UUID.randomUUID(), "sub", "p@test", null, "P", "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
  }

  @Test
  void raisePersistsPublishesEventAndConfirms() {
    when(sosEvents.save(any(SosEventEntity.class)))
        .thenAnswer(
            inv -> {
              SosEventEntity e = inv.getArgument(0);
              e.setId(3L);
              return e;
            });

    var res =
        service.raise("PASSENGER", new RaiseSosRequest(null, 7L, 6.9, 79.8, "Feeling unsafe"));

    assertThat(res.id()).isEqualTo(3L);
    assertThat(res.status()).isEqualTo("RAISED");
    verify(events).publish(any());
    verify(notifications).notifyUser(anyLong(), anyString(), anyString(), anyString(), any());
  }
}
