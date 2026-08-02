package com.routeshare.routing.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.routing.dto.request.CoordinateRequest;
import com.routeshare.routing.dto.request.RoutePublishRequest;
import com.routeshare.routing.dto.request.RouteSearchRequest;
import com.routeshare.routing.dto.response.RouteSearchResponse;
import com.routeshare.routing.service.RouteService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppFacingRouteAliasControllerTest {
  @Mock private RouteService routes;

  @Test
  void passengerRideSearchDelegatesToRouteSearchService() {
    var controller =
        new PassengerRideSearchController(
            routes,
            org.mockito.Mockito.mock(com.routeshare.common.ratelimit.RateLimiter.class),
            new com.routeshare.common.ratelimit.RateLimitProperties(
                false, null, null, null, null, null, null, null, null),
            currentUser());
    var request =
        new RouteSearchRequest(
            new CoordinateRequest(6.9271, 79.8612),
            new CoordinateRequest(6.9000, 79.9000),
            Instant.parse("2026-06-02T04:30:00Z"),
            1,
            20,
            null,
            "BEST_MATCH",
            0,
            10);
    var result =
        new RouteSearchResponse(
            10L,
            20L,
            "Rajagiriya",
            "Nugegoda",
            Instant.parse("2026-06-02T04:45:00Z"),
            3,
            12_000,
            0.10,
            0.80,
            100,
            120,
            8_000,
            70.0,
            88.0,
            "Strong route overlap",
            8_000L,
            new java.math.BigDecimal("980.00"),
            "LKR",
            null,
            "Nimal Perera",
            "Toyota",
            "Aqua",
            "CAB-1234",
            4,
            2.4,
            "MOST_OF_ROUTE",
            "Most of route",
            "8 km of your trip is on this driver's route (70%)",
            "Silver",
            "CAR",
            new java.math.BigDecimal("50.00"),
            new java.math.BigDecimal("38.00"),
            new java.math.BigDecimal("62.00"),
            "INSTANT",
            false,
            false);
    var page =
        new com.routeshare.routing.dto.response.RideSearchPageResponse(
            List.of(result), 1, 0, 20, 20, List.of(5, 10, 20), "BEST_MATCH", 0, 10, false);
    when(routes.search(request)).thenReturn(page);

    var response = controller.create(request);

    assertThat(response.success()).isTrue();
    assertThat(response.data().results()).containsExactly(result);
    verify(routes).search(request);
  }

  private static com.routeshare.common.security.CurrentUserProvider currentUser() {
    var provider =
        org.mockito.Mockito.mock(com.routeshare.common.security.CurrentUserProvider.class);
    when(provider.requireCurrentUser())
        .thenReturn(
            new com.routeshare.common.security.CurrentUser(
                "subject", "rider@example.test", null, "Rider", java.util.Set.of()));
    return provider;
  }

  @Test
  void driverRouteCreateDelegatesToRoutePublishService() {
    var controller = new DriverRouteController(routes);
    var request =
        new RoutePublishRequest(
            7L,
            "Rajagiriya",
            "Nugegoda",
            List.of(new CoordinateRequest(6.9271, 79.8612), new CoordinateRequest(6.9000, 79.9000)),
            3,
            Instant.parse("2026-06-02T04:45:00Z"));
    when(routes.publish(request)).thenReturn(Map.of("routePlanId", 10L, "routeOccurrenceId", 20L));

    var response = controller.create(request);

    assertThat(response.success()).isTrue();
    assertThat(response.data()).containsEntry("routePlanId", 10L);
    assertThat(response.data()).containsEntry("routeOccurrenceId", 20L);
    verify(routes).publish(request);
  }
}
