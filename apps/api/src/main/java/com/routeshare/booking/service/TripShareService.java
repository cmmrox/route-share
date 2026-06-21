package com.routeshare.booking.service;

import com.routeshare.booking.dto.request.ShareTripRequest;
import com.routeshare.booking.dto.response.PublicTripStatusResponse;
import com.routeshare.booking.dto.response.ShareTripResponse;

public interface TripShareService {
  ShareTripResponse share(long bookingId, ShareTripRequest req);

  void revoke(long bookingId, String token);

  PublicTripStatusResponse publicStatus(String token);
}
