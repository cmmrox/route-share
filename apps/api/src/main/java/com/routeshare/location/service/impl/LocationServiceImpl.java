package com.routeshare.location.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.location.dto.request.LocationUpdateRequest;
import com.routeshare.location.repository.LocationSampleRepository;
import com.routeshare.location.service.LocationService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class LocationServiceImpl implements LocationService {
  private static final Duration MAX_DEVICE_CLOCK_SKEW = Duration.ofMinutes(10);

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final LocationSampleRepository locations;
  private final Clock clock;

  public LocationServiceImpl(
      CurrentUserProvider current,
      IdentityFacade identityFacade,
      LocationSampleRepository locations) {
    this(current, identityFacade, locations, Clock.systemUTC());
  }

  @Transactional
  public Map<String, Object> update(LocationUpdateRequest req) {
    validateDeviceTimestamp(req.deviceRecordedAt());
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    if (!locations.canUpdateTripLocation(req.tripId(), req.driverProfileId(), app.appUserId())) {
      throw new AccessDeniedException("Trip does not belong to current driver or is not active");
    }
    locations.save(req);
    return Map.of("accepted", true);
  }

  private void validateDeviceTimestamp(Instant deviceRecordedAt) {
    if (deviceRecordedAt == null
        || Duration.between(deviceRecordedAt, Instant.now(clock))
                .abs()
                .compareTo(MAX_DEVICE_CLOCK_SKEW)
            > 0) {
      throw new IllegalArgumentException("Location timestamp is stale or too far in future");
    }
  }
}
