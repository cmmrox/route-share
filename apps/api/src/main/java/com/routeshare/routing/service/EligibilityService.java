package com.routeshare.routing.service;

import com.routeshare.routing.domain.EligibilityDecision;

/**
 * Who may ride with whom, decided by the server.
 *
 * <p>Applied in exactly two places — the search query and the booking guard — and both go through
 * this one service so they cannot disagree. A trip a rider cannot book must not appear in her
 * results at all: P36's "nobody wastes a request" is a promise that only holds if the filter and
 * the guard are the same rule.
 *
 * <p>Three inputs, no more: the trip's gender policy, the trip's verified-only flag and the rider's
 * profile. Nothing here reads a preference through to the driver's account, because a preference
 * changed after publication must not change the terms of a trip somebody has already booked.
 */
public interface EligibilityService {

  /**
   * The rule, for one rider and one trip.
   *
   * <p>Pure with respect to the caller: it records nothing and refuses nothing on its own. The
   * caller decides whether a denial is stated or silently applied.
   */
  EligibilityDecision canBook(long appUserId, long routeOccurrenceId);

  /**
   * Refuses the booking outright, with the typed reason. The booking guard's entry point.
   *
   * @throws com.routeshare.common.errors.GateDeniedException with {@code NOT_ELIGIBLE_WOMEN_ONLY}
   *     or {@code NOT_ELIGIBLE_VERIFIED_ONLY}
   */
  void requireEligible(long appUserId, long routeOccurrenceId);

  /**
   * True when this rider is verified — the search query needs it as a bind parameter, and asking
   * once beats asking per candidate row.
   */
  boolean isVerified(long appUserId);

  /** True when this rider's NIC verifies her as female. */
  boolean isVerifiedFemale(long appUserId);

  /**
   * Records the trips a search silently dropped.
   *
   * <p>Nothing is said to the rider — search omits, it does not explain. The rows exist for the
   * driver: D35 shows him what "verified riders only" actually cost, and an omission leaves no
   * other trace, because a rider who never saw the trip never made a request there is anything else
   * to count.
   */
  void recordSearchDenials(long appUserId, java.util.List<SearchExclusion> exclusions);

  record SearchExclusion(long routeOccurrenceId, String reason) {}
}
