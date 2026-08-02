package com.routeshare.reliability.domain;

/**
 * What happened. Counters are projections of these, never incremented in place — a correction must
 * be possible, and the D28/P39 panels must be able to show what happened rather than only a number.
 */
public enum ReliabilityEventType {
  /**
   * Driver: a trip auto-cancelled because the start buffer expired. Three in a month deactivate.
   */
  MISSED_START,
  /** Driver: cancelled a published trip inside the free window. */
  LATE_CANCELLATION,
  /** Driver: spent the single start extension. Not a penalty; D28 shows it as context. */
  START_EXTENSION_USED,
  /** Passenger: the pickup wait expired and the seat was released. */
  NO_SHOW,
  /** Passenger: cancelled inside the penalty window. */
  LATE_CANCEL,
  /** Passenger: an early drop-off that was fare-adjusted, against the monthly allowance. */
  EARLY_DROP_ADJUSTED,
  TRIP_COMPLETED,
  TRIP_BOOKED,
  ON_TIME,
  ON_TIME_OPPORTUNITY,
  /** An operator correction. Recorded, never a silent edit of a counter. */
  CORRECTION
}
