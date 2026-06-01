package com.routeshare.passenger.service;

import com.routeshare.passenger.dto.request.PassengerProfileRequest;
import com.routeshare.passenger.dto.response.PassengerProfileResponse;
import java.util.Optional;

public interface PassengerProfileService {
  PassengerProfileResponse upsert(PassengerProfileRequest req);

  Optional<PassengerProfileResponse> get();
}
