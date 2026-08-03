package com.routeshare.chat.service;

import com.routeshare.chat.dto.*;
import java.time.Instant;

public interface ChatService {
  void openForConfirmedBooking(long bookingId);

  void scheduleClose(long bookingId, Instant closesAt);

  int closeDue();

  ChatThreadResponse thread(long bookingId);

  ChatMessagesResponse messages(long bookingId, long since, int limit);

  ChatMessageResponse send(long bookingId, String idempotencyKey, ChatMessageRequest request);

  int markRead(long bookingId, ChatReadRequest request);

  ChatMessagesResponse adminMessages(long bookingId, String reason, long since, int limit);
}
