package com.routeshare.passenger.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.passenger.dto.request.UsualCommuteRequest;
import com.routeshare.passenger.dto.response.UsualCommuteResponse;
import com.routeshare.passenger.repository.UsualCommuteRepository;
import com.routeshare.passenger.service.UsualCommuteService;
import com.routeshare.routing.dto.request.CoordinateRequest;
import com.routeshare.routing.dto.request.RouteSearchRequest;
import com.routeshare.routing.service.RouteService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsualCommuteServiceImpl implements UsualCommuteService {

  /** Sri Lanka has one zone, and "08:15" on a dashboard means 08:15 where the rider is. */
  private static final ZoneId LOCAL_ZONE = ZoneId.of("Asia/Colombo");

  /** The window the dashboard count uses. Wide enough to be useful, narrow enough to mean today. */
  private static final int MATCH_WINDOW_MINUTES = 90;

  private final CurrentUserProvider current;
  private final IdentityFacade identity;
  private final UsualCommuteRepository commutes;
  private final RouteService routes;
  private final Clock clock;

  @Override
  @Transactional
  public UsualCommuteResponse mine() {
    long appUserId = currentAppUserId();
    return commutes
        .findByAppUserId(appUserId)
        .map(this::withLiveMatches)
        .orElseGet(UsualCommuteResponse::none);
  }

  @Override
  @Transactional
  public UsualCommuteResponse save(UsualCommuteRequest request) {
    long appUserId = currentAppUserId();
    commutes.upsert(
        appUserId,
        request.originLabel(),
        request.origin().latitude(),
        request.origin().longitude(),
        request.destinationLabel(),
        request.destination().latitude(),
        request.destination().longitude(),
        request.habitualTime());
    return mine();
  }

  @Override
  @Transactional
  public void clear() {
    commutes.clear(currentAppUserId());
  }

  /**
   * Runs the real search rather than counting rows.
   *
   * <p>Which means the dashboard inherits eligibility, the radius rule and the fare engine for free
   * — and, more importantly, can never show "4 drivers" over a list that then shows three.
   */
  private UsualCommuteResponse withLiveMatches(UsualCommuteRepository.UsualCommuteRow row) {
    var results =
        searchQuietly(
            new RouteSearchRequest(
                new CoordinateRequest(row.getOriginLatitude(), row.getOriginLongitude()),
                new CoordinateRequest(row.getDestinationLatitude(), row.getDestinationLongitude()),
                nextDeparture(row.getHabitualTime()),
                1,
                null,
                MATCH_WINDOW_MINUTES,
                "BEST_MATCH",
                0,
                20));
    var best = results == null || results.results().isEmpty() ? null : results.results().getFirst();
    return new UsualCommuteResponse(
        true,
        row.getOriginLabel(),
        row.getOriginLatitude(),
        row.getOriginLongitude(),
        row.getDestinationLabel(),
        row.getDestinationLatitude(),
        row.getDestinationLongitude(),
        row.getHabitualTime() == null ? null : row.getHabitualTime().toLocalTime().toString(),
        results == null ? 0 : (int) Math.min(Integer.MAX_VALUE, results.totalMatching()),
        best == null
            ? null
            : new UsualCommuteResponse.BestMatch(
                best.routeOccurrenceId(),
                best.driverName(),
                best.overlapPercent(),
                best.matchTier(),
                best.estimatedFare(),
                best.departureTime()));
  }

  /**
   * A dashboard is a glance, not a transaction. If search is unhealthy the card should say "no
   * matches right now" rather than take the whole home screen down with it.
   */
  private com.routeshare.routing.dto.response.RideSearchPageResponse searchQuietly(
      RouteSearchRequest request) {
    try {
      return routes.search(request);
    } catch (RuntimeException ex) {
      log.warn("usual commute: live match count unavailable", ex);
      return null;
    }
  }

  /**
   * The next time she would actually leave.
   *
   * <p>Searching from "now" would show an evening commuter nothing all day; searching from a time
   * already past today would show her yesterday's trips. So today's slot if it is still ahead,
   * tomorrow's otherwise.
   */
  private Instant nextDeparture(java.sql.Time habitualTime) {
    Instant now = clock.instant();
    if (habitualTime == null) {
      return now.plus(Duration.ofMinutes(15));
    }
    LocalTime local = habitualTime.toLocalTime();
    LocalDate today = LocalDate.ofInstant(now, LOCAL_ZONE);
    Instant todaySlot = today.atTime(local).atZone(LOCAL_ZONE).toInstant();
    return todaySlot.isAfter(now)
        ? todaySlot
        : today.plusDays(1).atTime(local).atZone(LOCAL_ZONE).toInstant();
  }

  private long currentAppUserId() {
    return identity.upsertFromToken(current.requireCurrentUser()).appUserId();
  }
}
