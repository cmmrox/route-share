package com.routeshare.booking.service;

import com.routeshare.booking.dto.request.BookingRequest;
import java.util.Map;

public interface BookingService {
  Map<String, Object> book(BookingRequest req, String idempotencyKey);
}
