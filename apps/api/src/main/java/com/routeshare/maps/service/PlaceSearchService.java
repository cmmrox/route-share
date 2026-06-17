package com.routeshare.maps.service;

import com.routeshare.maps.dto.PlaceSuggestionResponse;
import java.util.List;

public interface PlaceSearchService {
  List<PlaceSuggestionResponse> autocomplete(String query, Double latitude, Double longitude);

  PlaceSuggestionResponse details(String placeId);
}
