package com.routeshare.routing.service.impl;

import com.routeshare.maps.service.PlaceSearchService;
import com.routeshare.routing.dto.response.PickupPointResponse;
import com.routeshare.routing.entity.PickupPointEntity;
import com.routeshare.routing.repository.PickupPointRepository;
import com.routeshare.routing.service.PickupPointService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PickupPointServiceImpl implements PickupPointService {

  /**
   * How close a known point has to be to stand in for the coordinate a rider dropped.
   *
   * <p>Curated points reach further because an operator chose them deliberately — a named junction
   * is worth walking eighty metres to, and it is a better instruction than an address forty metres
   * nearer. Derived points are tighter, because the only thing recommending them is proximity.
   */
  private static final int CURATED_RADIUS_METERS = 250;

  private static final int PERSISTED_RADIUS_METERS = 60;

  /** A route endpoint further than this is a different place, whatever it is called. */
  private static final int ROUTE_LABEL_RADIUS_METERS = 80;

  private static final int PLACES_RADIUS_METERS = 150;

  private final PickupPointRepository points;
  private final PlaceSearchService places;
  private final MeterRegistry meters;

  @Override
  @Transactional
  public PickupPointResponse resolve(double latitude, double longitude) {
    // 1 — curated. Free, and the only tier that carries a real landmark name.
    var curated = points.findNearestCurated(latitude, longitude, CURATED_RADIUS_METERS);
    if (curated.isPresent()) {
      return hit("curated", toResponse(curated.get()));
    }

    // 2 — already resolved at this corner by somebody else. Free, and the reason the steady-state
    // cost of this feature tends to nothing.
    var persisted = points.findNearestPersisted(latitude, longitude, PERSISTED_RADIUS_METERS);
    if (persisted.isPresent()) {
      return hit("persisted", toResponse(persisted.get()));
    }

    // 3 — a name Places was already paid for when a driver published a route through here.
    var routeLabel = points.findNearestRouteLabel(latitude, longitude, ROUTE_LABEL_RADIUS_METERS);
    if (routeLabel.isPresent() && !routeLabel.get().isBlank()) {
      return hit(
          "route_label",
          persist(
              routeLabel.get(),
              "Where drivers on this corridor already start and finish.",
              latitude,
              longitude,
              PickupPointEntity.SOURCE_DERIVED,
              null));
    }

    // 4 — the billable call. Made once for this corner and then never again, because the row it
    // writes is what step 2 finds next time.
    var nearby = places.nearestLandmark(latitude, longitude, PLACES_RADIUS_METERS);
    if (nearby.isPresent()) {
      var place = nearby.get();
      return hit(
          "places",
          persist(
              place.address() == null || place.address().isBlank()
                  ? place.label()
                  : place.address(),
              "Nearest recognisable spot to the pin you dropped.",
              place.coordinate() == null ? latitude : place.coordinate().latitude(),
              place.coordinate() == null ? longitude : place.coordinate().longitude(),
              PickupPointEntity.SOURCE_DERIVED,
              place.placeId() == null || place.placeId().isBlank() ? null : place.placeId()));
    }

    // 5 — no name for it. A booking must never depend on Google being reachable, so this always
    // succeeds and the rider gets a coordinate she can at least read out.
    return hit(
        "raw",
        persist(
            generatedLabel(latitude, longitude),
            "We couldn't name this spot. Agree a landmark with your driver in chat.",
            latitude,
            longitude,
            PickupPointEntity.SOURCE_DERIVED,
            null));
  }

  @Override
  @Transactional
  public void recordUse(long pickupPointId) {
    points.recordUse(pickupPointId);
  }

  @Override
  @Transactional(readOnly = true)
  public java.util.List<PickupPointResponse> list(String source) {
    return points.listBySource(source).stream().map(PickupPointServiceImpl::toResponse).toList();
  }

  @Override
  @Transactional
  public PickupPointResponse createCurated(
      com.routeshare.routing.dto.request.PickupPointRequest request, long actorAppUserId) {
    long id =
        points.insertPoint(
            request.label(),
            request.description(),
            request.sideHint(),
            request.position().latitude(),
            request.position().longitude(),
            PickupPointEntity.SOURCE_CURATED,
            null,
            actorAppUserId);
    return points
        .findRow(id)
        .map(PickupPointServiceImpl::toResponse)
        .orElseThrow(
            () -> new IllegalStateException("Curated pickup point could not be read back"));
  }

  /**
   * The hit rate per tier, which is the number that says whether the cost model is holding. Places
   * should trend toward zero as the library fills; a flat Places rate means the reuse radius is too
   * tight or the curated seed never landed.
   */
  private PickupPointResponse hit(String tier, PickupPointResponse response) {
    meters.counter("routeshare_pickup_point_resolutions_total", "tier", tier).increment();
    return response;
  }

  private PickupPointResponse persist(
      String label,
      String description,
      double latitude,
      double longitude,
      String source,
      String googlePlaceId) {
    long id =
        points.insertPoint(
            label, description, null, latitude, longitude, source, googlePlaceId, null);
    return points
        .findRow(id)
        .map(PickupPointServiceImpl::toResponse)
        .orElseGet(
            () ->
                new PickupPointResponse(id, label, description, null, latitude, longitude, source));
  }

  private static String generatedLabel(double latitude, double longitude) {
    return String.format(Locale.ROOT, "Pin at %.5f, %.5f", latitude, longitude);
  }

  private static PickupPointResponse toResponse(PickupPointRepository.PickupPointRow row) {
    return new PickupPointResponse(
        row.getPickupPointId(),
        row.getLabel(),
        row.getDescription(),
        row.getSideHint(),
        row.getLatitude(),
        row.getLongitude(),
        row.getSource());
  }
}
