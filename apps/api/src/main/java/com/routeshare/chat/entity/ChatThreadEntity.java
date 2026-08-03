package com.routeshare.chat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_thread", schema = "chat")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatThreadEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "chat_thread_id")
  private Long id;

  @Column(name = "booking_id", nullable = false, unique = true)
  private Long bookingId;

  @Column(nullable = false)
  private String state;

  @Column(name = "opened_at", nullable = false)
  private Instant openedAt;

  @Column(name = "closes_at")
  private Instant closesAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  public static ChatThreadEntity open(long bookingId, Instant now) {
    var thread = new ChatThreadEntity();
    thread.bookingId = bookingId;
    thread.state = "OPEN";
    thread.openedAt = now;
    return thread;
  }

  public void scheduleClose(Instant closesAt) {
    if ("OPEN".equals(state)) {
      this.closesAt = closesAt;
    }
  }

  public boolean close(Instant now) {
    if (!"OPEN".equals(state)) {
      return false;
    }
    state = "CLOSED";
    closedAt = now;
    return true;
  }
}
