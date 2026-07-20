package com.routeshare.maps.service;

import com.routeshare.maps.dto.PlaceSuggestionResponse;
import java.util.List;

public interface PlaceSearchService {
  /**
   * Type-ahead place suggestions. {@code sessionToken} is the client-generated Google Places
   * autocomplete session token; passing the same token for the keystrokes of one search and the
   * terminating details call groups them under Google's cheaper session billing.
   */
  List<PlaceSuggestionResponse> autocomplete(
      String query, Double latitude, Double longitude, String sessionToken);

  PlaceSuggestionResponse details(String placeId, String sessionToken);
}
