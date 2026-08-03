package com.routeshare.chat.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.routeshare.chat.entity.ChatThreadEntity;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class ChatLifecycleIT {
  private static final Instant NOW = Instant.parse("2026-08-03T10:00:00Z");

  @Test
  void confirmationOpensOneThreadAndDropoffSchedulesClosureExactlyTwentyFourHoursLater() {
    var fixture = new ChatTestFixture(NOW, 1L);
    when(fixture.threads.findByBookingId(77L)).thenReturn(Optional.empty());
    when(fixture.threads.saveAndFlush(any(ChatThreadEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    fixture.service.openForConfirmedBooking(77L);

    var captured = org.mockito.ArgumentCaptor.forClass(ChatThreadEntity.class);
    verify(fixture.threads).saveAndFlush(captured.capture());
    assertThat(captured.getValue().getBookingId()).isEqualTo(77L);
    assertThat(captured.getValue().getState()).isEqualTo("OPEN");

    var thread = ChatThreadEntity.open(77L, NOW);
    when(fixture.threads.findByBookingId(77L)).thenReturn(Optional.of(thread));
    fixture.service.scheduleClose(77L, NOW.plusSeconds(24 * 60 * 60));

    assertThat(thread.getClosesAt()).isEqualTo(NOW.plusSeconds(24 * 60 * 60));
  }

  @Test
  void dueThreadClosesAndFutureThreadRemainsOpen() {
    var fixture = new ChatTestFixture(NOW, 1L);
    var due = ChatThreadEntity.open(77L, NOW.minusSeconds(48 * 60 * 60));
    due.scheduleClose(NOW);
    var future = ChatThreadEntity.open(78L, NOW);
    future.scheduleClose(NOW.plusSeconds(60));
    when(fixture.threads.findTop200ByStateAndClosesAtLessThanEqualOrderByClosesAt("OPEN", NOW))
        .thenReturn(List.of(due));

    assertThat(fixture.service.closeDue()).isOne();
    assertThat(due.getState()).isEqualTo("CLOSED");
    assertThat(future.getState()).isEqualTo("OPEN");
  }
}
