package com.routeshare.booking.service;

import com.routeshare.booking.dto.request.EarlyDropOffRequest;
import com.routeshare.booking.dto.response.EarlyDropOffResponse;

public interface EarlyDropOffService {
  EarlyDropOffResponse finalizeEarlyDropOff(long bookingId, EarlyDropOffRequest req);
}
