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

  /**
   * The nearest thing worth standing next to, for slice 09's pickup points.
   *
   * <p>Uses the Essentials field mask only — {@code id}, {@code formattedAddress} and {@code
   * location}. A landmark <em>name</em> lives in {@code displayName}, which is a Pro-tier field,
   * and one Pro field upgrades the whole request to Pro pricing. So a derived point is labelled by
   * its address; real landmark names come from the curated tier, which is exactly what the curated
   * seed exists for.
   *
   * @return empty when Places is disabled, unavailable, or has nothing near enough — the caller
   *     falls back to a generated label rather than failing the booking
   */
  java.util.Optional<PlaceSuggestionResponse> nearestLandmark(
      double latitude, double longitude, int radiusMeters);
}
