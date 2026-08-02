package com.routeshare.trip.service;

/**
 * Brings a trip into existence for an occurrence that has just taken its first confirmed booking.
 *
 * <p>Publication does not create trips. It creates a route plan and a run of occurrences, which are
 * offers; most of them will never carry anybody. A trip is the thing that has a driver, a clock and
 * money attached, and it earns that status at the moment somebody books a seat on it.
 *
 * <p>Creating one per generated occurrence instead would put every unbooked occurrence under the
 * start-buffer sweeper, and each one would auto-cancel and record a missed start against a driver
 * who did nothing wrong — three days of an empty recurring route would deactivate them.
 */
public interface TripLifecycleService {

  /**
   * The trip for this occurrence, created with its start window if this is the first booking.
   *
   * <p>Idempotent and safe under concurrency: two passengers taking the last two seats at the same
   * instant are two transactions racing on one occurrence, and only one row can win {@code
   * trip_route_occurrence_uk}. Both callers get the same trip id.
   */
  long ensureTripForBookedOccurrence(long routeOccurrenceId);
}
