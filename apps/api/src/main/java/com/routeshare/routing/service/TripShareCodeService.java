package com.routeshare.routing.service;

import com.routeshare.routing.dto.response.TripShareCodeResponse;

/** D14 — the short link and QR a driver hands round for a published trip. */
public interface TripShareCodeService {

  /** Idempotent: asking twice returns the same code, so a link already shared keeps working. */
  TripShareCodeResponse shareFor(long routeOccurrenceId);

  /** Returns the current share metadata without creating a code. */
  TripShareCodeResponse getFor(long routeOccurrenceId);

  /** Stops the code resolving. The row stays, so the history of the share survives. */
  TripShareCodeResponse revoke(long routeOccurrenceId);

  /**
   * The occurrence behind a code.
   *
   * @throws java.util.NoSuchElementException for an unknown <em>or revoked</em> code — a 404 either
   *     way, because 410 would confirm the code once existed and that is the one bit an enumerator
   *     wants
   */
  long resolve(String shortCode);

  /** The QR for a code, rendered in-process and cached. */
  byte[] qrPng(String shortCode);
}
