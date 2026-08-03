package com.routeshare.support.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.support.dto.CreateTicketRequest;
import com.routeshare.support.dto.TicketMessageRequest;
import com.routeshare.support.entity.SupportMessageEntity;
import com.routeshare.support.entity.SupportTicketEntity;
import com.routeshare.support.repository.SupportMessageRepository;
import com.routeshare.support.repository.SupportTicketRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class SupportServiceImplTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final SupportTicketRepository tickets = mock(SupportTicketRepository.class);
  private final SupportMessageRepository messages = mock(SupportMessageRepository.class);
  private final SupportServiceImpl service =
      new SupportServiceImpl(
          current,
          identityFacade,
          tickets,
          messages,
          mock(com.routeshare.support.repository.SupportAttachmentRepository.class),
          mock(com.routeshare.storage.service.ObjectStoragePort.class),
          new com.routeshare.storage.config.ObjectStorageProperties(
              false, null, null, "test", null, null, true, 900),
          java.time.Clock.systemUTC(),
          10_485_760L,
          "image/jpeg,image/png,application/pdf");

  @BeforeEach
  void setUp() {
    var user = new CurrentUser("sub", "p@test", null, "P", Set.of("PASSENGER"));
    var appUser = new AppUser(5L, UUID.randomUUID(), "sub", "p@test", null, "P", "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
  }

  @Test
  void createPersistsTicketAndInitialMessage() {
    when(tickets.save(any(SupportTicketEntity.class)))
        .thenAnswer(
            inv -> {
              SupportTicketEntity t = inv.getArgument(0);
              t.setId(10L);
              return t;
            });

    var res =
        service.create(
            "PASSENGER",
            new CreateTicketRequest("Payment issue", "PAYMENT", "HIGH", "Charged twice"));

    assertThat(res.id()).isEqualTo(10L);
    assertThat(res.status()).isEqualTo("OPEN");
    assertThat(res.priority()).isEqualTo("HIGH");
    verify(messages).save(any(SupportMessageEntity.class));
  }

  @Test
  void addMessageReopensResolvedTicket() {
    var ticket = SupportTicketEntity.open(5L, "PASSENGER", "S", "GENERAL", "NORMAL");
    ticket.setId(10L);
    ticket.setStatus(SupportTicketEntity.RESOLVED);
    when(tickets.findByIdAndAppUserId(10L, 5L)).thenReturn(Optional.of(ticket));
    when(messages.save(any(SupportMessageEntity.class)))
        .thenAnswer(
            inv -> {
              SupportMessageEntity m = inv.getArgument(0);
              m.setId(1L);
              return m;
            });

    service.addMessage("PASSENGER", 10L, new TicketMessageRequest("Any update?"));

    assertThat(ticket.getStatus()).isEqualTo(SupportTicketEntity.OPEN);
  }

  @Test
  void getMineDeniedForOthersTicket() {
    when(tickets.findByIdAndAppUserId(99L, 5L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getMine(99L)).isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void listMineReturnsOwnedTickets() {
    var ticket = SupportTicketEntity.open(5L, "PASSENGER", "S", "GENERAL", "NORMAL");
    ticket.setId(11L);
    when(tickets.findByAppUserIdOrderByIdDesc(5L)).thenReturn(List.of(ticket));
    assertThat(service.listMine()).hasSize(1);
  }
}
