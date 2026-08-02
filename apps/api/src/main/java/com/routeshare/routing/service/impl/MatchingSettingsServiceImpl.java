package com.routeshare.routing.service.impl;

import com.routeshare.routing.dto.request.MatchingSettingsRequest;
import com.routeshare.routing.dto.response.MatchingSettingsResponse;
import com.routeshare.routing.entity.MatchingSettingsEntity;
import com.routeshare.routing.repository.MatchingSettingsRepository;
import com.routeshare.routing.service.MatchingSettingsService;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchingSettingsServiceImpl implements MatchingSettingsService {
  // Fallbacks if the singleton row is somehow absent. They mirror the migration defaults, so a
  // missing row degrades to the product's stated rule rather than to nothing.
  static final int DEFAULT_TRIP_START_RADIUS_METERS = 20_000;
  static final int MAX_TRIP_START_RADIUS_METERS = 20_000;
  static final List<Integer> DEFAULT_ALLOWED_RADII_METERS = List.of(5_000, 10_000, 20_000);
  static final int DEFAULT_DEPARTURE_WINDOW_MINUTES = 120;
  static final int MAX_DEPARTURE_WINDOW_MINUTES = 720;

  /**
   * The ceiling is a product decision rather than a tuning limit: past 20 km a driver is making a
   * trip for the rider instead of sharing one, and no operator setting should be able to say
   * otherwise.
   */
  static final int PRODUCT_RADIUS_CEILING_METERS = 20_000;

  private final MatchingSettingsRepository settings;

  @Override
  @Transactional(readOnly = true)
  public MatchingSettingsResponse get() {
    return settings.current().map(MatchingSettingsServiceImpl::toResponse).orElseGet(defaults());
  }

  @Override
  @Transactional
  public MatchingSettingsResponse update(MatchingSettingsRequest req) {
    if (req.defaultTripStartRadiusMeters() > req.maxTripStartRadiusMeters()) {
      throw new IllegalArgumentException(
          "Default trip-start radius cannot exceed the maximum trip-start radius");
    }
    if (req.maxTripStartRadiusMeters() > PRODUCT_RADIUS_CEILING_METERS) {
      throw new IllegalArgumentException(
          "Trip-start radius cannot exceed 20 km: beyond that a driver is making a trip for the"
              + " rider rather than sharing one");
    }
    if (!req.allowedTripStartRadiiMeters().contains(req.defaultTripStartRadiusMeters())) {
      // A default the screen cannot offer would render as no chip selected at all.
      throw new IllegalArgumentException(
          "The default trip-start radius must be one of the offered options");
    }
    req.allowedTripStartRadiiMeters().stream()
        .filter(radius -> radius > req.maxTripStartRadiusMeters())
        .findFirst()
        .ifPresent(
            radius -> {
              throw new IllegalArgumentException(
                  "Offered radius " + radius + " m exceeds the maximum");
            });
    if (req.defaultDepartureWindowMinutes() > req.maxDepartureWindowMinutes()) {
      throw new IllegalArgumentException(
          "Default departure window cannot exceed the maximum departure window");
    }
    var entity = settings.current().orElseGet(MatchingSettingsEntity::newSingleton);
    entity.setDefaultTripStartRadiusMeters(req.defaultTripStartRadiusMeters());
    entity.setMaxTripStartRadiusMeters(req.maxTripStartRadiusMeters());
    entity.setAllowedTripStartRadiiMeters(
        req.allowedTripStartRadiiMeters().stream().mapToInt(Integer::intValue).sorted().toArray());
    entity.setDefaultDepartureWindowMinutes(req.defaultDepartureWindowMinutes());
    entity.setMaxDepartureWindowMinutes(req.maxDepartureWindowMinutes());
    entity.setUpdatedAt(Instant.now());
    return toResponse(settings.save(entity));
  }

  private static java.util.function.Supplier<MatchingSettingsResponse> defaults() {
    return () ->
        new MatchingSettingsResponse(
            DEFAULT_TRIP_START_RADIUS_METERS,
            MAX_TRIP_START_RADIUS_METERS,
            DEFAULT_ALLOWED_RADII_METERS,
            DEFAULT_DEPARTURE_WINDOW_MINUTES,
            MAX_DEPARTURE_WINDOW_MINUTES);
  }

  private static MatchingSettingsResponse toResponse(MatchingSettingsEntity e) {
    return new MatchingSettingsResponse(
        e.getDefaultTripStartRadiusMeters(),
        e.getMaxTripStartRadiusMeters(),
        e.getAllowedTripStartRadiiMeters() == null
            ? DEFAULT_ALLOWED_RADII_METERS
            : Arrays.stream(e.getAllowedTripStartRadiiMeters()).boxed().toList(),
        e.getDefaultDepartureWindowMinutes(),
        e.getMaxDepartureWindowMinutes());
  }
}
