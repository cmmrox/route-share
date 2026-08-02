package com.routeshare.penalty.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * One penalty, as P26, P27, D21, D30, D31 and D41 need to state it.
 *
 * <p>Every figure those screens name is here — the base, the percentage, the fee, the victim's half
 * and the platform's — because a screen that has to derive one of them is a screen that will
 * eventually derive it differently from the ledger.
 */
public record PenaltyResponse(
    long id,
    String kind,
    Long bookingId,
    Long tripId,
    BigDecimal fareBase,
    BigDecimal percent,
    BigDecimal feeAmount,
    BigDecimal victimShare,
    BigDecimal platformShare,
    String payerRole,
    String victimRole,
    /** Positive when this penalty paid the caller, negative when it charged them. */
    BigDecimal amountForViewer,
    Collection collection,
    String disputeState,
    boolean disputable,
    Instant assessedAt,
    String explanation,
    List<Beneficiary> beneficiaries) {

  public record Collection(String method, String status, Instant settledAt) {}

  /**
   * A beneficiary, named only by first name. D31 tells a driver his fee reached "Dinuka and
   * Tharindu"; a surname or a contact detail would turn a fee notice into a disclosure.
   */
  public record Beneficiary(String firstName, BigDecimal amount) {}
}
