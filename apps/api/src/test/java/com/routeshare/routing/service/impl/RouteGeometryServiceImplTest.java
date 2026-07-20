package com.routeshare.routing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.routing.repository.RoutePlanRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RouteGeometryServiceImplTest {

  private record SegmentRow(String geoJson, Double lengthMeters)
      implements RoutePlanRepository.RouteSegmentRow {
    @Override
    public String getGeoJson() {
      return geoJson;
    }

    @Override
    public Double getLengthMeters() {
      return lengthMeters;
    }
  }

  @Test
  void returnsStoredRouteSegmentCoordinatesAndLength() {
    var routes = mock(RoutePlanRepository.class);
    when(routes.findOccurrenceSegment(11L, 0.2, 0.8))
        .thenReturn(
            Optional.of(
                new SegmentRow(
                    "{\"type\":\"LineString\",\"coordinates\":[[79.85,6.93],[79.87,6.90],[79.90,6.86]]}",
                    5321.4)));
    var service = new RouteGeometryServiceImpl(routes, new ObjectMapper());

    var geometry = service.occurrenceSegment(11L, 0.2, 0.8);

    assertThat(geometry.coordinates()).hasSize(3);
    assertThat(geometry.coordinates().get(0).latitude()).isEqualTo(6.93);
    assertThat(geometry.coordinates().get(0).longitude()).isEqualTo(79.85);
    assertThat(geometry.distanceMeters()).isEqualTo(5321L);
    assertThat(geometry.source()).isEqualTo("route_plan");
  }

  @Test
  void missingOccurrenceIsNotFound() {
    var routes = mock(RoutePlanRepository.class);
    when(routes.findOccurrenceSegment(anyLong(), anyDouble(), anyDouble()))
        .thenReturn(Optional.empty());
    var service = new RouteGeometryServiceImpl(routes, new ObjectMapper());

    assertThatThrownBy(() -> service.occurrenceSegment(99L, 0.1, 0.9))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void degeneratePointSegmentIsNotFound() {
    var routes = mock(RoutePlanRepository.class);
    when(routes.findOccurrenceSegment(anyLong(), anyDouble(), anyDouble()))
        .thenReturn(
            Optional.of(new SegmentRow("{\"type\":\"Point\",\"coordinates\":[79.85,6.93]}", 0.0)));
    var service = new RouteGeometryServiceImpl(routes, new ObjectMapper());

    assertThatThrownBy(() -> service.occurrenceSegment(11L, 0.5, 0.6))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void invalidFractionsAreRejectedBeforeTouchingTheDatabase() {
    var routes = mock(RoutePlanRepository.class);
    var service = new RouteGeometryServiceImpl(routes, new ObjectMapper());

    assertThatThrownBy(() -> service.occurrenceSegment(11L, -0.1, 0.5))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.occurrenceSegment(11L, 0.8, 0.2))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.occurrenceSegment(11L, 0.5, 0.5))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(routes);
  }
}
