package com.routeshare.passenger.dto.request;

import jakarta.validation.constraints.*;
import java.util.Map;

public record PassengerProfileRequest(
    @NotBlank @Size(max = 120) String fullName,
    String photoUrl,
    Map<String, Object> preferences,
    @Pattern(regexp = "^[A-Za-z2-9]{6,20}$") String referralCode) {
  public PassengerProfileRequest(
      String fullName, String photoUrl, Map<String, Object> preferences) {
    this(fullName, photoUrl, preferences, null);
  }
}
