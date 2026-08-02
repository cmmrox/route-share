package com.routeshare.penalty.service;

import com.routeshare.penalty.dto.request.PenaltyDisputeDecisionRequest;
import com.routeshare.penalty.dto.request.PenaltyDisputeRequest;
import com.routeshare.penalty.dto.response.PenaltyDisputeResponse;
import com.routeshare.penalty.dto.response.PenaltyResponse;
import java.util.List;
import java.util.Optional;

/**
 * Prices broken promises, splits each fee in half, and moves both halves.
 *
 * <p>Slice 05 decides that something happened — a seat released, a grace expired, a start missed.
 * This decides what it costs and who is owed. The two are separated on purpose: whether a no-show
 * occurred is a question about evidence, and what it costs is a question about policy, and they
 * change for different reasons.
 *
 * <p>Every assessment is idempotent through the database. A sweeper that runs twice, or two sweeps
 * racing on the same released seat, must cost the passenger once.
 */
public interface PenaltyService {

  /** Her seat was released because she never boarded (slice 05's pickup-wait expiry). */
  Optional<PenaltyResponse> assessPassengerNoShow(long bookingId, Long tripId);

  /** She cancelled once the car was already moving. */
  Optional<PenaltyResponse> assessPassengerCancelAfterStart(long bookingId, Long tripId);

  /** He was late past the grace and she took the free cancel it unlocked. */
  Optional<PenaltyResponse> assessDriverLate(long bookingId);

  /** He cancelled a published trip inside the free window, stranding everyone booked on it. */
  Optional<PenaltyResponse> assessDriverLateCancellation(long tripId);

  /** He never started and the trip auto-cancelled. No fee (D32b); the record is the consequence. */
  Optional<PenaltyResponse> recordDriverMissedStart(long tripId);

  /** Both directions: what this user was charged, and what somebody else's penalty paid them. */
  List<PenaltyResponse> listForUser(long appUserId);

  PenaltyResponse dispute(long penaltyId, long appUserId, PenaltyDisputeRequest request);

  List<PenaltyResponse> adminSearch(String kind, String status);

  List<PenaltyDisputeResponse> adminDisputes(String status);

  PenaltyDisputeResponse decide(
      long disputeId, long adminAppUserId, PenaltyDisputeDecisionRequest request);
}
