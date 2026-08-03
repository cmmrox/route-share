package com.routeshare.chat.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.routeshare.booking.facade.BookingFacade;
import com.routeshare.chat.dto.ChatMessageRequest;
import com.routeshare.chat.entity.ChatThreadEntity;
import com.routeshare.common.errors.GateConflictException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ChatAuthorizationTest {
  private static final Instant NOW = Instant.parse("2026-08-03T10:00:00Z");

  @Test
  void unrelatedUserCannotReadAConfirmedBookingThread() {
    var fixture = new ChatTestFixture(NOW, 99L);
    when(fixture.bookings.findChatContext(77L))
        .thenReturn(Optional.of(new BookingFacade.ChatContext(77L, 1L, 2L, "CONFIRMED", null)));

    assertThatThrownBy(() -> fixture.service.thread(77L)).isInstanceOf(AccessDeniedException.class);
    verify(fixture.threads, never()).findByBookingId(anyLong());
  }

  @Test
  void messageBeforeConfirmationIsUnavailableAndPostingAfterCloseIsRefused() {
    var fixture = new ChatTestFixture(NOW, 1L);
    when(fixture.bookings.findChatContext(77L))
        .thenReturn(Optional.of(new BookingFacade.ChatContext(77L, 1L, 2L, "CONFIRMED", null)));
    when(fixture.threads.findByBookingId(77L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> fixture.service.thread(77L))
        .isInstanceOf(GateConflictException.class)
        .hasMessageContaining("available after");

    var closed = ChatThreadEntity.open(77L, NOW.minusSeconds(48 * 60 * 60));
    closed.close(NOW);
    when(fixture.threads.findByBookingId(77L)).thenReturn(Optional.of(closed));

    assertThatThrownBy(
            () ->
                fixture.service.send(
                    77L, "stable-idempotency-key", new ChatMessageRequest("Hello")))
        .isInstanceOf(GateConflictException.class)
        .hasMessageContaining("closed");
  }
}
