package com.routeshare.chat.facade.impl;

import com.routeshare.chat.facade.ChatFacade;
import com.routeshare.chat.service.ChatService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatFacadeImpl implements ChatFacade {
  private final ChatService chat;

  @Override
  public void openForConfirmedBooking(long bookingId) {
    chat.openForConfirmedBooking(bookingId);
  }

  @Override
  public void scheduleClose(long bookingId, Instant closesAt) {
    chat.scheduleClose(bookingId, closesAt);
  }
}
