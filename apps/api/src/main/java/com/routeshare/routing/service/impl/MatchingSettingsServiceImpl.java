package com.routeshare.routing.service.impl;

import com.routeshare.routing.dto.request.MatchingSettingsRequest;
import com.routeshare.routing.dto.response.MatchingSettingsResponse;
import com.routeshare.routing.entity.MatchingSettingsEntity;
import com.routeshare.routing.repository.MatchingSettingsRepository;
import com.routeshare.routing.service.MatchingSettingsService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchingSettingsServiceImpl implements MatchingSettingsService {
  // Fallback defaults if the singleton row is somehow absent (mirror the migration defaults).
  static final int DEFAULT_SEARCH_RADIUS_METERS = 1_000;
  static final int MAX_SEARCH_RADIUS_METERS = 5_000;
  static final int DEFAULT_DEPARTURE_WINDOW_MINUTES = 120;
  static final int MAX_DEPARTURE_WINDOW_MINUTES = 720;

  private final MatchingSettingsRepository settings;

  @Override
  @Transactional(readOnly = true)
  public MatchingSettingsResponse get() {
    return settings.current().map(MatchingSettingsServiceImpl::toResponse).orElseGet(defaults());
  }

  @Override
  @Transactional
  public MatchingSettingsResponse update(MatchingSettingsRequest req) {
    if (req.defaultSearchRadiusMeters() > req.maxSearchRadiusMeters()) {
      throw new IllegalArgumentException(
          "Default search radius cannot exceed the maximum search radius");
    }
    if (req.defaultDepartureWindowMinutes() > req.maxDepartureWindowMinutes()) {
      throw new IllegalArgumentException(
          "Default departure window cannot exceed the maximum departure window");
    }
    var entity = settings.current().orElseGet(MatchingSettingsEntity::newSingleton);
    entity.setDefaultSearchRadiusMeters(req.defaultSearchRadiusMeters());
    entity.setMaxSearchRadiusMeters(req.maxSearchRadiusMeters());
    entity.setDefaultDepartureWindowMinutes(req.defaultDepartureWindowMinutes());
    entity.setMaxDepartureWindowMinutes(req.maxDepartureWindowMinutes());
    entity.setUpdatedAt(Instant.now());
    return toResponse(settings.save(entity));
  }

  private static java.util.function.Supplier<MatchingSettingsResponse> defaults() {
    return () ->
        new MatchingSettingsResponse(
            DEFAULT_SEARCH_RADIUS_METERS,
            MAX_SEARCH_RADIUS_METERS,
            DEFAULT_DEPARTURE_WINDOW_MINUTES,
            MAX_DEPARTURE_WINDOW_MINUTES);
  }

  private static MatchingSettingsResponse toResponse(MatchingSettingsEntity e) {
    return new MatchingSettingsResponse(
        e.getDefaultSearchRadiusMeters(),
        e.getMaxSearchRadiusMeters(),
        e.getDefaultDepartureWindowMinutes(),
        e.getMaxDepartureWindowMinutes());
  }
}
