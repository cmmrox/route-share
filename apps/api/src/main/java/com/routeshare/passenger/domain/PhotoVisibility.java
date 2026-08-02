package com.routeshare.passenger.domain;

/**
 * P30/P30b/P30c — who may see a rider's face.
 *
 * <p>The asymmetry is deliberate: a rider may hide her photo from everyone including her driver,
 * and a driver may not, because she is getting into his car and has to know it is him (D35). So
 * this enum governs riders; a driver's photo is always returned to a confirmed rider and never in
 * search.
 */
public enum PhotoVisibility {
  /** Anyone signed in, including a search result. */
  PUBLIC,

  /** Only the counterparty of a confirmed booking. The default. */
  MATCHED,

  /** Nobody. The URL is never emitted, not even for a client to ignore. */
  HIDDEN;

  public static PhotoVisibility of(String value) {
    return value == null || value.isBlank() ? MATCHED : PhotoVisibility.valueOf(value);
  }
}
