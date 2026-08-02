package com.routeshare.routing.service;

import com.routeshare.routing.dto.response.PickupPointResponse;

/**
 * Turns a coordinate into somewhere a driver can find a rider.
 *
 * <p>The resolution chain is ordered by cost, and the order is the feature. Resolved naively this
 * is the plan's single largest new Google line item — roughly 30,000 Place Details calls a month at
 * 500 trips a day, about $150, enough on its own to break the monthly credit. Each step exists to
 * stop the next one being reached:
 *
 * <ol>
 *   <li><b>Curated</b> — an operator-maintained landmark. Free, and the only tier with real names.
 *   <li><b>Persisted</b> — a point already resolved at this corner. Free, and the hit rate climbs
 *       toward 100% as the library fills, because a city has a finite number of sensible corners.
 *   <li><b>Route label</b> — a name Places was already paid for when a driver published a route.
 *   <li><b>Places</b> — a billable call, made once per corner and then persisted so it is never
 *       made again.
 *   <li><b>Raw</b> — a generated label. Never fails, because a booking must not depend on Google.
 * </ol>
 *
 * <p>Resolution happens once, at booking. Never per search keystroke and never per location ping.
 */
public interface PickupPointService {

  /** Resolves and persists, so the next rider at this corner costs nothing. */
  PickupPointResponse resolve(double latitude, double longitude);

  /** Records that a point was actually used — what tier 3 will later promote on. */
  void recordUse(long pickupPointId);

  /** Operator listing, optionally narrowed to one tier. */
  java.util.List<PickupPointResponse> list(String source);

  /**
   * Adds a curated landmark.
   *
   * <p>The only tier that carries a real name: a derived point can only be labelled by its address,
   * because a Places {@code displayName} is Pro-tier and one Pro field re-prices the whole request.
   */
  PickupPointResponse createCurated(
      com.routeshare.routing.dto.request.PickupPointRequest request, long actorAppUserId);
}
