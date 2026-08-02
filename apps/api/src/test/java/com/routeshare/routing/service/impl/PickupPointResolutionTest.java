package com.routeshare.routing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.maps.dto.CoordinateResponse;
import com.routeshare.maps.dto.PlaceSuggestionResponse;
import com.routeshare.maps.service.PlaceSearchService;
import com.routeshare.routing.entity.PickupPointEntity;
import com.routeshare.routing.repository.PickupPointRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The order of the resolution chain, which is the whole cost model of the feature.
 *
 * <p>Resolved naively this is the plan's single largest new Google line item — about $150 a month
 * at launch volumes, enough on its own to break the credit. Every assertion here is really the same
 * assertion: that Places is genuinely last, and that anything it does return is written down so it
 * is never asked twice.
 */
class PickupPointResolutionTest {

  private static final double LAT = 6.90890;
  private static final double LNG = 79.89400;

  private final PickupPointRepository points = mock(PickupPointRepository.class);
  private final PlaceSearchService places = mock(PlaceSearchService.class);

  private final PickupPointServiceImpl service =
      new PickupPointServiceImpl(points, places, new SimpleMeterRegistry());

  @BeforeEach
  void nothingKnownAndNothingNearby() {
    when(points.findNearestCurated(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(Optional.empty());
    when(points.findNearestPersisted(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(Optional.empty());
    when(points.findNearestRouteLabel(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(Optional.empty());
    when(places.nearestLandmark(anyDouble(), anyDouble(), anyInt())).thenReturn(Optional.empty());
    when(points.insertPoint(
            anyString(), any(), any(), anyDouble(), anyDouble(), anyString(), any(), any()))
        .thenReturn(1L);
    when(points.findRow(anyLong())).thenReturn(Optional.empty());
  }

  @Test
  @DisplayName("09-20: a curated point beside the pin wins, and nothing else is consulted")
  void curatedWinsAndShortCircuits() {
    var curated = row("Rajagiriya junction bus halt", "CURATED");
    when(points.findNearestCurated(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(Optional.of(curated));

    var resolved = service.resolve(LAT, LNG);

    assertThat(resolved.label()).isEqualTo("Rajagiriya junction bus halt");
    assertThat(resolved.source()).isEqualTo("CURATED");
    verify(points, never()).findNearestPersisted(anyDouble(), anyDouble(), anyInt());
    verify(places, never()).nearestLandmark(anyDouble(), anyDouble(), anyInt());
  }

  @Test
  @DisplayName("09-22: a corner somebody already resolved is reused, with no second Places call")
  void persistedPointIsReused() {
    var known = row("150 Nawala Road", "DERIVED");
    when(points.findNearestPersisted(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(Optional.of(known));

    var resolved = service.resolve(LAT, LNG);

    assertThat(resolved.label()).isEqualTo("150 Nawala Road");
    verify(places, never()).nearestLandmark(anyDouble(), anyDouble(), anyInt());
    // Nothing new is written either — a second row for the same corner would defeat the reuse it
    // was meant to provide.
    verify(points, never())
        .insertPoint(
            anyString(), any(), any(), anyDouble(), anyDouble(), anyString(), any(), any());
  }

  @Test
  @DisplayName("09-28: a coordinate at an existing route's origin uses that label, free")
  void routeLabelBeatsPlaces() {
    when(points.findNearestRouteLabel(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(Optional.of("Colombo Fort"));

    var resolved = service.resolve(LAT, LNG);

    assertThat(resolved.label()).isEqualTo("Colombo Fort");
    // The name was already paid for when a driver published a route through here.
    verify(places, never()).nearestLandmark(anyDouble(), anyDouble(), anyInt());
  }

  @Test
  @DisplayName("09-21: with nothing known, Places is called — once — and the answer is persisted")
  void placesIsTheLastResortAndIsWrittenDown() {
    when(places.nearestLandmark(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(
            Optional.of(
                new PlaceSuggestionResponse(
                    "place-123",
                    "42 Nawala Road",
                    "42 Nawala Road",
                    new CoordinateResponse(LAT, LNG))));

    var resolved = service.resolve(LAT, LNG);

    assertThat(resolved.label()).isEqualTo("42 Nawala Road");
    verify(places).nearestLandmark(anyDouble(), anyDouble(), anyInt());
    // The place id is stored so the unique index refuses a duplicate, and so the next rider at this
    // corner is served by the persisted tier instead.
    verify(points)
        .insertPoint(
            eq("42 Nawala Road"),
            any(),
            any(),
            anyDouble(),
            anyDouble(),
            eq(PickupPointEntity.SOURCE_DERIVED),
            eq("place-123"),
            any());
  }

  @Test
  @DisplayName(
      "09-23: Places returning nothing still yields a usable point — a booking must not fail")
  void placesSilenceFallsBackToTheRawPin() {
    var resolved = service.resolve(LAT, LNG);

    assertThat(resolved.label()).contains("6.90890").contains("79.89400");
    assertThat(resolved.description()).contains("Agree a landmark");
  }

  @Test
  @DisplayName("a Places failure surfaces here and is absorbed by the caller, not swallowed twice")
  void placesFailurePropagatesToTheBookingLayer() {
    when(places.nearestLandmark(anyDouble(), anyDouble(), anyInt()))
        .thenThrow(new RuntimeException("Google is down"));

    // The adapter already degrades a Google outage to an empty result, so anything reaching here
    // is a real fault. Swallowing it a second time would hide a broken chain behind a coordinate
    // label that looked deliberate. The booking path is where it becomes non-fatal — a pickup
    // point is a nicety on top of a booking that must still succeed.
    org.junit.jupiter.api.Assertions.assertThrows(
        RuntimeException.class, () -> service.resolve(LAT, LNG));
  }

  private static PickupPointRepository.PickupPointRow row(String label, String source) {
    var row = mock(PickupPointRepository.PickupPointRow.class);
    when(row.getPickupPointId()).thenReturn(7L);
    when(row.getLabel()).thenReturn(label);
    when(row.getSource()).thenReturn(source);
    when(row.getLatitude()).thenReturn(LAT);
    when(row.getLongitude()).thenReturn(LNG);
    return row;
  }
}
