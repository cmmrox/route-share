package com.routeshare.routing.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.routing.domain.RouteBucketCellGenerator;
import com.routeshare.routing.domain.RouteMatchCandidate;
import com.routeshare.routing.domain.RouteMatchScorer;
import com.routeshare.routing.domain.RouteSchedulePolicy;
import com.routeshare.routing.dto.request.CoordinateRequest;
import com.routeshare.routing.dto.request.RecurringRoutePublishRequest;
import com.routeshare.routing.dto.request.RoutePublishRequest;
import com.routeshare.routing.dto.request.RouteSearchRequest;
import com.routeshare.routing.dto.response.DriverRouteResponse;
import com.routeshare.routing.dto.response.RecurringRouteResponse;
import com.routeshare.routing.dto.response.RouteSearchResponse;
import com.routeshare.routing.repository.MatchingSettingsRepository;
import com.routeshare.routing.repository.RouteBucketCellRepository;
import com.routeshare.routing.repository.RouteOccurrenceRepository;
import com.routeshare.routing.repository.RoutePlanRepository;
import com.routeshare.routing.repository.RouteScheduleRuleRepository;
import com.routeshare.routing.service.RouteService;
import com.routeshare.vehicle.facade.VehicleFacade;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@lombok.extern.slf4j.Slf4j
public class RouteServiceImpl implements RouteService {
  public static final int MIN_ROUTE_POINTS = 2;
  public static final int MAX_ROUTE_POINTS = 500;
  private static final int DEFAULT_SEARCH_RADIUS_METERS = 1_000;
  private static final int DEFAULT_DEPARTURE_WINDOW_MINUTES = 120;
  private static final int DEFAULT_SEARCH_LIMIT = 20;
  private static final int ROUTE_BUCKET_RESOLUTION = 3;

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final DriverFacade driverFacade;
  private final VehicleFacade vehicleFacade;
  private final RoutePlanRepository routes;
  private final Clock clock;
  private final RouteMatchScorer routeMatchScorer;
  private final RouteSchedulePolicy routeSchedulePolicy;
  private final RouteBucketCellGenerator routeBucketCellGenerator;
  private final RouteScheduleRuleRepository scheduleRules;
  private final RouteOccurrenceRepository occurrences;
  private final RouteBucketCellRepository bucketCells;
  private final MatchingSettingsRepository matchingSettings;
  private final com.routeshare.pricing.facade.PricingFacade pricing;
  private final com.routeshare.routing.service.SeatInventoryService seatInventory;

  public RouteServiceImpl(
      CurrentUserProvider current,
      IdentityFacade identityFacade,
      DriverFacade driverFacade,
      VehicleFacade vehicleFacade,
      RoutePlanRepository routes) {
    this(current, identityFacade, driverFacade, vehicleFacade, routes, Clock.systemUTC());
  }

  public RouteServiceImpl(
      CurrentUserProvider current,
      IdentityFacade identityFacade,
      DriverFacade driverFacade,
      VehicleFacade vehicleFacade,
      RoutePlanRepository routes,
      Clock clock) {
    this(
        current,
        identityFacade,
        driverFacade,
        vehicleFacade,
        routes,
        clock,
        new RouteMatchScorer(),
        new RouteSchedulePolicy(clock),
        new RouteBucketCellGenerator(),
        null,
        null,
        null,
        null,
        null,
        null);
  }

  @Transactional
  public Map<String, Object> publish(RoutePublishRequest req) {
    validateCoordinates(req.coordinates());
    var generatedOccurrences = routeSchedulePolicy.generateOneTimeOccurrences(req.departureTime());
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    long driverId =
        driverFacade
            .findApprovedDriverProfileIdByAppUserId(app.appUserId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Approved driver profile is required before publishing routes"));
    requireApprovedVehicleWithCapacity(req.vehicleId(), driverId, req.availableSeats());
    long routePlanId =
        routes.create(
            driverId,
            req.vehicleId(),
            req.originLabel(),
            req.destinationLabel(),
            toLineStringPoints(req.coordinates()),
            req.departureTime(),
            req.availableSeats());
    long scheduleRuleId = scheduleRules.insertOneTimeRule(routePlanId, req.departureTime());
    long routeOccurrenceId =
        occurrences.insertOccurrence(
            routePlanId, generatedOccurrences.getFirst(), req.availableSeats());
    // The seats exist from the moment the trip does. Generating them lazily at first booking would
    // make the seat picker empty on a trip nobody has booked yet — which is every new trip.
    seatInventory.generateFor(routeOccurrenceId);
    routeBucketCellGenerator
        .cellsFor(req.coordinates(), ROUTE_BUCKET_RESOLUTION)
        .forEach(
            cell ->
                bucketCells.insertCell(
                    routePlanId, routeOccurrenceId, ROUTE_BUCKET_RESOLUTION, cell));
    return Map.of(
        "routePlanId",
        routePlanId,
        "routeScheduleRuleId",
        scheduleRuleId,
        "routeOccurrenceId",
        routeOccurrenceId);
  }

  @Transactional
  public List<RouteSearchResponse> search(RouteSearchRequest req) {
    validateSearchRequest(req);
    identityFacade.upsertFromToken(current.requireCurrentUser());
    MatchingDefaults defaults = matchingDefaults();
    int pickupRadiusMeters =
        clamp(
            valueOrDefault(req.pickupRadiusMeters(), defaults.defaultSearchRadiusMeters()),
            defaults.maxSearchRadiusMeters());
    int dropoffRadiusMeters =
        clamp(
            valueOrDefault(req.dropoffRadiusMeters(), defaults.defaultSearchRadiusMeters()),
            defaults.maxSearchRadiusMeters());
    int departureWindowMinutes =
        clamp(
            valueOrDefault(req.departureWindowMinutes(), defaults.defaultDepartureWindowMinutes()),
            defaults.maxDepartureWindowMinutes());
    int limit = valueOrDefault(req.limit(), DEFAULT_SEARCH_LIMIT);
    Instant windowStart =
        req.requestedDepartureTime().minus(Duration.ofMinutes(departureWindowMinutes));
    Instant windowEnd =
        req.requestedDepartureTime().plus(Duration.ofMinutes(departureWindowMinutes));

    return routes
        .findSearchCandidates(
            req.pickup().longitude(),
            req.pickup().latitude(),
            req.dropoff().longitude(),
            req.dropoff().latitude(),
            windowStart,
            windowEnd,
            req.seats(),
            pickupRadiusMeters,
            dropoffRadiusMeters,
            ROUTE_BUCKET_RESOLUTION,
            routeBucketCellGenerator.cellFor(req.pickup(), ROUTE_BUCKET_RESOLUTION),
            routeBucketCellGenerator.cellFor(req.dropoff(), ROUTE_BUCKET_RESOLUTION),
            limit)
        .stream()
        .map(row -> toSearchResponse(row, req.seats()))
        .sorted(Comparator.comparing(RouteSearchResponse::score).reversed())
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DriverRouteResponse> listDriverRoutes() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return routes.findDriverRoutes(app.appUserId()).stream()
        .map(this::toDriverRouteResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public DriverRouteResponse getDriverRoute(long routeId) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return routes
        .findDriverRoute(app.appUserId(), routeId)
        .map(this::toDriverRouteResponse)
        .orElseThrow(() -> new java.util.NoSuchElementException("Route not found"));
  }

  @Override
  @Transactional
  public Map<String, Object> cancelDriverRoute(long routeId) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    int updated = routes.cancelDriverRoutePlan(app.appUserId(), routeId);
    if (updated != 1) {
      throw new java.util.NoSuchElementException("Route not found or not cancellable");
    }
    routes.cancelRouteOccurrences(routeId);
    return Map.of("routePlanId", routeId, "status", "CANCELLED");
  }

  @Override
  @Transactional
  public Map<String, Object> createShareLink(long routeId) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    if (!routes.isPublishedDriverRoute(app.appUserId(), routeId)) {
      throw new java.util.NoSuchElementException("Route not found or not shareable");
    }
    String token = UUID.randomUUID().toString();
    String shareUrl = "https://routeshare.local/routes/" + routeId + "?share=" + token;
    String qrPayload = "ROUTESHARE_ROUTE:" + token;
    String savedUrl = routes.upsertShareLink(routeId, token, shareUrl, qrPayload);
    return Map.of("routeId", routeId, "shareUrl", savedUrl, "qrPayload", qrPayload);
  }

  private static final int DEFAULT_RECURRING_HORIZON_DAYS = 30;
  private static final int MAX_RECURRING_HORIZON_DAYS = 90;

  @Override
  @Transactional
  public Map<String, Object> publishRecurring(RecurringRoutePublishRequest req) {
    validateCoordinates(req.coordinates());
    Set<DayOfWeek> days = parseDays(req.daysOfWeek());
    Instant generateUntil = horizonFrom(req.firstDepartureTime(), req.horizonDays());
    var departures =
        routeSchedulePolicy.generateRecurringOccurrences(
            req.firstDepartureTime(),
            req.endAt(),
            days,
            generateUntil,
            null,
            RouteSchedulePolicy.MAX_RECURRING_OCCURRENCES);
    if (departures.isEmpty()) {
      throw new IllegalArgumentException(
          "Recurring schedule produced no future departures in the horizon");
    }
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    long driverId =
        driverFacade
            .findApprovedDriverProfileIdByAppUserId(app.appUserId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Approved driver profile is required before publishing routes"));
    requireApprovedVehicleWithCapacity(req.vehicleId(), driverId, req.availableSeats());
    long routePlanId =
        routes.create(
            driverId,
            req.vehicleId(),
            req.originLabel(),
            req.destinationLabel(),
            toLineStringPoints(req.coordinates()),
            departures.getFirst(),
            req.availableSeats());
    long ruleId =
        scheduleRules.insertRecurringRule(
            routePlanId, req.firstDepartureTime(), req.endAt(), daysToCsv(days));
    var cells = routeBucketCellGenerator.cellsFor(req.coordinates(), ROUTE_BUCKET_RESOLUTION);
    List<Long> occurrenceIds = new ArrayList<>();
    for (Instant departure : departures) {
      long occurrenceId =
          seatInventory.generateFor(
              occurrences.insertOccurrence(routePlanId, departure, req.availableSeats()));
      occurrenceIds.add(occurrenceId);
      cells.forEach(
          cell -> bucketCells.insertCell(routePlanId, occurrenceId, ROUTE_BUCKET_RESOLUTION, cell));
    }
    return Map.of(
        "routePlanId",
        routePlanId,
        "routeScheduleRuleId",
        ruleId,
        "generatedOccurrences",
        occurrenceIds.size(),
        "firstDeparture",
        departures.getFirst());
  }

  @Override
  @Transactional(readOnly = true)
  public List<RecurringRouteResponse> listRecurringRoutes() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return scheduleRules.findRecurringRulesForDriver(app.appUserId()).stream()
        .map(this::toRecurringResponse)
        .toList();
  }

  @Override
  @Transactional
  public RecurringRouteResponse updateRecurringStatus(long ruleId, String status) {
    if (!Set.of("ACTIVE", "PAUSED", "CANCELLED").contains(status)) {
      throw new IllegalArgumentException("Invalid recurring route status: " + status);
    }
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    var rule =
        scheduleRules
            .findRecurringRuleForDriver(ruleId, app.appUserId())
            .orElseThrow(() -> new java.util.NoSuchElementException("Recurring route not found"));
    scheduleRules.updateStatus(ruleId, status);
    if ("CANCELLED".equals(status)) {
      routes.cancelRouteOccurrences(rule.getRoutePlanId());
    }
    return scheduleRules
        .findRecurringRuleForDriver(ruleId, app.appUserId())
        .map(this::toRecurringResponse)
        .orElseThrow();
  }

  @Override
  @Transactional
  public Map<String, Object> generateRecurringOccurrences(long ruleId, Integer horizonDays) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    var rule =
        scheduleRules
            .findRecurringRuleForDriver(ruleId, app.appUserId())
            .orElseThrow(() -> new java.util.NoSuchElementException("Recurring route not found"));
    if (!"ACTIVE".equals(rule.getStatus())) {
      throw new IllegalStateException("Only active recurring routes can generate occurrences");
    }
    var latest =
        occurrences
            .findLatestForPlan(rule.getRoutePlanId())
            .orElseThrow(
                () ->
                    new IllegalStateException("Recurring route has no occurrences to extend from"));
    Instant generateUntil = horizonFrom(Instant.now(clock), horizonDays);
    var departures =
        routeSchedulePolicy.generateRecurringOccurrences(
            rule.getStartAt(),
            rule.getEndAt(),
            parseDays(splitCsv(rule.getDaysCsv())),
            generateUntil,
            latest.getScheduledAt(),
            RouteSchedulePolicy.MAX_RECURRING_OCCURRENCES);
    int generated = 0;
    for (Instant departure : departures) {
      long occurrenceId =
          occurrences.insertOccurrence(
              rule.getRoutePlanId(), departure, latest.getAvailableSeats());
      seatInventory.generateFor(occurrenceId);
      bucketCells.copyCellsToOccurrence(latest.getOccurrenceId(), occurrenceId);
      generated++;
    }
    return Map.of(
        "routeScheduleRuleId", ruleId,
        "routePlanId", rule.getRoutePlanId(),
        "generatedOccurrences", generated);
  }

  private RecurringRouteResponse toRecurringResponse(
      RouteScheduleRuleRepository.RecurringRuleRow row) {
    return new RecurringRouteResponse(
        row.getRuleId(),
        row.getRoutePlanId(),
        row.getOriginLabel(),
        row.getDestinationLabel(),
        row.getStartAt(),
        row.getEndAt(),
        splitCsv(row.getDaysCsv()),
        row.getStatus());
  }

  private Instant horizonFrom(Instant from, Integer horizonDays) {
    int days =
        horizonDays == null || horizonDays <= 0
            ? DEFAULT_RECURRING_HORIZON_DAYS
            : Math.min(horizonDays, MAX_RECURRING_HORIZON_DAYS);
    return from.plus(Duration.ofDays(days));
  }

  private Set<DayOfWeek> parseDays(List<String> days) {
    Set<DayOfWeek> result = new LinkedHashSet<>();
    if (days == null) {
      return result;
    }
    for (String day : days) {
      if (day == null || day.isBlank()) {
        continue;
      }
      switch (day.trim().toUpperCase()) {
        case "MON", "MONDAY" -> result.add(DayOfWeek.MONDAY);
        case "TUE", "TUESDAY" -> result.add(DayOfWeek.TUESDAY);
        case "WED", "WEDNESDAY" -> result.add(DayOfWeek.WEDNESDAY);
        case "THU", "THURSDAY" -> result.add(DayOfWeek.THURSDAY);
        case "FRI", "FRIDAY" -> result.add(DayOfWeek.FRIDAY);
        case "SAT", "SATURDAY" -> result.add(DayOfWeek.SATURDAY);
        case "SUN", "SUNDAY" -> result.add(DayOfWeek.SUNDAY);
        default -> throw new IllegalArgumentException("Invalid day of week: " + day);
      }
    }
    return result;
  }

  private String daysToCsv(Set<DayOfWeek> days) {
    return days.stream()
        .map(d -> d.name().substring(0, 3))
        .reduce((a, b) -> a + "," + b)
        .orElse("");
  }

  private List<String> splitCsv(String csv) {
    if (csv == null || csv.isBlank()) {
      return List.of();
    }
    return List.of(csv.split(","));
  }

  private DriverRouteResponse toDriverRouteResponse(RoutePlanRepository.DriverRouteRow row) {
    return new DriverRouteResponse(
        row.getRoutePlanId(),
        row.getRouteOccurrenceId(),
        row.getVehicleId(),
        row.getOriginLabel(),
        row.getDestinationLabel(),
        row.getDepartureTime(),
        row.getAvailableSeats(),
        row.getRouteLengthMeters(),
        row.getRouteStatus(),
        row.getOccurrenceStatus());
  }

  public void validateCoordinates(List<CoordinateRequest> coordinates) {
    if (coordinates == null || coordinates.size() < MIN_ROUTE_POINTS) {
      throw new IllegalArgumentException("Route must contain at least two coordinates");
    }
    if (coordinates.size() > MAX_ROUTE_POINTS) {
      throw new IllegalArgumentException("Route must not contain more than 500 coordinates");
    }
    CoordinateRequest previous = null;
    boolean hasMovement = false;
    for (CoordinateRequest coordinate : coordinates) {
      validateCoordinate(coordinate);
      if (previous != null && !samePoint(previous, coordinate)) {
        hasMovement = true;
      }
      previous = coordinate;
    }
    if (!hasMovement) {
      throw new IllegalArgumentException("Route must contain at least two distinct coordinates");
    }
  }

  private void validateSearchRequest(RouteSearchRequest req) {
    validateCoordinate(req.pickup());
    validateCoordinate(req.dropoff());
    if (samePoint(req.pickup(), req.dropoff())) {
      throw new IllegalArgumentException("Search pickup and drop-off must be different");
    }
    if (req.requestedDepartureTime() == null
        || req.requestedDepartureTime().isBefore(Instant.now(clock))) {
      throw new IllegalArgumentException("Search departure time must be now or in the future");
    }
    if (req.seats() < 1) {
      throw new IllegalArgumentException("At least one seat is required");
    }
  }

  private RouteSearchResponse toSearchResponse(
      RoutePlanRepository.RouteSearchCandidateRow row, int seats) {
    var candidate =
        new RouteMatchCandidate(
            row.getRoutePlanId(),
            row.getOriginLabel(),
            row.getDestinationLabel(),
            row.getAvailableSeats(),
            row.getRouteLengthMeters(),
            row.getPickupFraction(),
            row.getDropoffFraction(),
            row.getPickupDistanceMeters(),
            row.getDropoffDistanceMeters(),
            row.getOverlapDistanceMeters(),
            row.getRequestedDistanceMeters());
    var score = routeMatchScorer.score(candidate);
    // Matched distance is the on-route segment the passenger actually travels; the same figure
    // prices the booking, so a result and its checkout can never disagree.
    long matchedMeters = Math.max(0, Math.round(row.getOverlapDistanceMeters()));
    int requestedSeats = Math.max(1, seats);
    com.routeshare.pricing.dto.response.FareQuoteResponse fare = null;
    java.math.BigDecimal estimatedFare = null;
    if (row.getVehicleId() != null) {
      try {
        var quote =
            pricing.quoteForMatch(
                row.getRouteOccurrenceId(),
                row.getVehicleId(),
                java.math.BigDecimal.valueOf(matchedMeters),
                java.math.BigDecimal.valueOf(score.overlapPercent()),
                requestedSeats);
        fare = com.routeshare.pricing.dto.response.FareQuoteResponse.forPassenger(quote);
        estimatedFare = quote.passengerPays();
      } catch (RuntimeException ex) {
        // A vehicle with no live band has no legal price. Showing it unpriced is better than
        // failing the whole search, and the publish gate should have kept it out anyway.
        log.warn(
            "search candidate omitted its fare: routeOccurrenceId={} vehicleId={}",
            row.getRouteOccurrenceId(),
            row.getVehicleId(),
            ex);
      }
    }
    return new RouteSearchResponse(
        row.getRoutePlanId(),
        row.getRouteOccurrenceId(),
        row.getOriginLabel(),
        row.getDestinationLabel(),
        row.getDepartureTime(),
        row.getAvailableSeats(),
        round(row.getRouteLengthMeters()),
        row.getPickupFraction(),
        row.getDropoffFraction(),
        round(row.getPickupDistanceMeters()),
        round(row.getDropoffDistanceMeters()),
        round(row.getOverlapDistanceMeters()),
        score.overlapPercent(),
        score.score(),
        score.explanation(),
        matchedMeters,
        estimatedFare,
        "LKR",
        fare,
        row.getDriverName(),
        row.getVehicleMake(),
        row.getVehicleModel(),
        row.getVehicleRegistration(),
        row.getVehicleSeatCount());
  }

  private void validateCoordinate(CoordinateRequest coordinate) {
    if (coordinate == null || coordinate.latitude() == null || coordinate.longitude() == null) {
      throw new IllegalArgumentException(
          "Invalid route coordinate. Latitude and longitude are required");
    }
    double latitude = coordinate.latitude();
    double longitude = coordinate.longitude();
    if (!Double.isFinite(latitude)
        || !Double.isFinite(longitude)
        || latitude < -90
        || latitude > 90
        || longitude < -180
        || longitude > 180) {
      throw new IllegalArgumentException(
          "Invalid route coordinate. Expected latitude/longitude in WGS84 bounds");
    }
  }

  private boolean samePoint(CoordinateRequest left, CoordinateRequest right) {
    return Double.compare(left.latitude(), right.latitude()) == 0
        && Double.compare(left.longitude(), right.longitude()) == 0;
  }

  private void requireApprovedVehicleWithCapacity(
      long vehicleId, long driverId, int requestedSeats) {
    if (!vehicleFacade.existsApprovedOwnedVehicleWithCapacity(
        vehicleId, driverId, requestedSeats)) {
      throw new IllegalStateException(
          "Approved vehicle does not belong to current driver or lacks capacity");
    }
  }

  private int valueOrDefault(Integer value, int defaultValue) {
    return value == null ? defaultValue : value;
  }

  private int clamp(int value, int max) {
    return Math.min(Math.max(value, 1), max);
  }

  private MatchingDefaults matchingDefaults() {
    if (matchingSettings != null) {
      return matchingSettings
          .current()
          .map(
              s ->
                  new MatchingDefaults(
                      s.getDefaultSearchRadiusMeters(),
                      s.getMaxSearchRadiusMeters(),
                      s.getDefaultDepartureWindowMinutes(),
                      s.getMaxDepartureWindowMinutes()))
          .orElseGet(RouteServiceImpl::fallbackMatchingDefaults);
    }
    return fallbackMatchingDefaults();
  }

  private static MatchingDefaults fallbackMatchingDefaults() {
    return new MatchingDefaults(
        DEFAULT_SEARCH_RADIUS_METERS,
        DEFAULT_SEARCH_RADIUS_METERS * 5,
        DEFAULT_DEPARTURE_WINDOW_MINUTES,
        DEFAULT_DEPARTURE_WINDOW_MINUTES * 6);
  }

  private record MatchingDefaults(
      int defaultSearchRadiusMeters,
      int maxSearchRadiusMeters,
      int defaultDepartureWindowMinutes,
      int maxDepartureWindowMinutes) {}

  private double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  private String toLineStringPoints(List<CoordinateRequest> coordinates) {
    return coordinates.stream()
        .map(coordinate -> coordinate.longitude() + " " + coordinate.latitude())
        .reduce((left, right) -> left + "," + right)
        .orElseThrow();
  }
}
