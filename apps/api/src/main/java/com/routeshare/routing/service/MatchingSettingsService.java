package com.routeshare.routing.service;

import com.routeshare.routing.dto.request.MatchingSettingsRequest;
import com.routeshare.routing.dto.response.MatchingSettingsResponse;

public interface MatchingSettingsService {
  MatchingSettingsResponse get();

  MatchingSettingsResponse update(MatchingSettingsRequest req);
}
