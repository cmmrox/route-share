package com.routeshare.routing.service;

import com.routeshare.routing.dto.request.ApprovalModeRequest;
import com.routeshare.routing.dto.request.OccurrenceCancellationRequest;
import com.routeshare.routing.dto.response.AlternativeTripResponse;
import com.routeshare.routing.dto.response.OccurrenceCancellationTermsResponse;
import com.routeshare.routing.dto.response.OccurrenceEditabilityResponse;
import java.util.List;
import java.util.Map;

/**
 * What a driver can still change about a published trip, and what it costs to call it off.
 *
 * <p>Two rules run through all of it. A trip freezes the moment somebody is riding on it (D09),
 * because editing a departure under a passenger who has already paid is the same as cancelling
 * without saying so. And the cancellation window is decided here but <b>priced by slice 06</b> — if
 * both modules knew the percentage, a screen and a ledger would eventually disagree about the same
 * fee.
 */
public interface OccurrenceLifecycleService {

  /** D13. Refused once the trip is frozen: who may book is part of the deal already struck. */
  Map<String, Object> setApprovalMode(long routeOccurrenceId, ApprovalModeRequest request);

  /**
   * D13's per-trip who-can-ride, overriding the account-level answer from D35.
   *
   * <p>Gated by the same rule as the preference — a driver whose NIC does not verify her as female
   * cannot set women-only here either, or the per-trip path would be a way round the account one —
   * and frozen with everything else, because who may book is part of the deal the first rider
   * already accepted.
   */
  Map<String, Object> setEligibility(
      long routeOccurrenceId,
      com.routeshare.routing.dto.request.OccurrenceEligibilityRequest request);

  /** D09's banner, and the predicate every edit path checks. */
  OccurrenceEditabilityResponse editability(long routeOccurrenceId);

  /** Throws {@code TRIP_FROZEN} when the occurrence already carries a live seat hold. */
  void requireEditable(long routeOccurrenceId);

  /** D30, before he commits to it: the window, the priced penalty and who it reaches. */
  OccurrenceCancellationTermsResponse cancellationTerms(long routeOccurrenceId);

  /** D30/D31. Cancels the trip, voids every booking, prices the penalty and tells every rider. */
  Map<String, Object> cancel(long routeOccurrenceId, OccurrenceCancellationRequest request);

  /** P13, P22, P24 — other trips on the corridor when this one falls through. */
  List<AlternativeTripResponse> alternatives(long routeOccurrenceId);
}
