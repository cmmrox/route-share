package com.routeshare.chat.facade;

import java.time.Instant;

public interface ChatFacade {
  void openForConfirmedBooking(long bookingId);

  void scheduleClose(long bookingId, Instant closesAt);
}
