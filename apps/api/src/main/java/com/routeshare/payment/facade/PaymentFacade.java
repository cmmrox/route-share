package com.routeshare.payment.facade;

import com.routeshare.payment.domain.CaptureOutcome;
import java.math.BigDecimal;
import java.util.List;

/**
 * Money movement, as the rest of the product needs it.
 *
 * <p>The promise this exists to keep is stated on eleven screens: <b>the card is authorised at
 * booking and captured when the driver starts the trip</b> — never on acceptance, never on
 * approval, and not at all if the trip does not happen.
 */
public interface PaymentFacade {
  /**
   * Holds the fare on the passenger's card at booking. Cash bookings create no intent at all: there
   * is nothing to hold, and pretending otherwise would put a fake row in a real ledger.
   */
  void authorizeForBooking(long bookingId, Long paymentMethodId, BigDecimal amount);

  /**
   * Captures every confirmed booking on a trip, once each.
   *
   * <p>A retried start must capture nothing further, and one declined bank must not stop the trip:
   * the driver is already at the wheel and the other passengers are already in the car. Each
   * booking therefore reports its own outcome.
   */
  List<CaptureOutcome> captureForTripStart(long tripId);

  /**
   * Releases a hold without charging. Cancel before start, decline, driver cancel, route cancel and
   * (from slice 05) the start-buffer auto-cancel all land here.
   */
  void voidForBooking(long bookingId, String reason);

  /**
   * Settles a fare that changed after the ride began — an early drop-off. Captures the lower amount
   * if nothing has been taken yet, refunds the difference if it has.
   */
  void settleRepricedFare(long bookingId, BigDecimal finalAmount);

  /**
   * Records the platform's cut on a cash fare as owed by the driver, to net from the next payout.
   */
  void recordCashCommissionOwed(long bookingId, BigDecimal fareCollected);

  /**
   * Takes a passenger's penalty fee by whichever path her booking allows, and says which one it
   * used.
   *
   * <p>The order is not a preference, it is a ranking by how little it disturbs her: money already
   * held is netted and the balance returned (P27's "the rest of the fare comes back to your Visa");
   * a live authorisation with nothing taken is charged down to the fee; and a cash booking, where
   * there is no instrument at all, falls to dues.
   *
   * <p>Only {@code payment} can tell which of those is true, so the decision lives here rather than
   * in the penalty module guessing at intent state.
   */
  PenaltyCollection collectPassengerPenalty(long bookingId, BigDecimal feeAmount);

  /**
   * A driver's penalty, as a negative ledger line against the trip that caused it.
   *
   * <p>He is never billed. D24 and D31 both say the fee comes out of what he earns next, and
   * charging a card for it would be a different product than the one the copy describes.
   */
  void recordDriverPenaltyDeduction(long bookingId, BigDecimal amount);

  /**
   * A driver's half of somebody else's penalty, as a positive line of its own kind.
   *
   * <p>D26 gives it a separate icon because folding it into fares would overstate what he earned
   * from driving. A passenger victim is not paid this way — her half is ride credit (P22), which is
   * the rewards balance, not this ledger.
   */
  void creditDriverCompensation(long bookingId, BigDecimal amount);

  /** Records that a checkout cleared carried-over dues, for the receipt that shows the line. */
  void recordDuesSettlement(long bookingId, BigDecimal amount);

  /** A reversed passenger penalty: the fee goes back to her card, and the ledger says so. */
  void reversePassengerPenalty(long bookingId, BigDecimal amount);

  /** A reversed driver penalty: the deduction is given back against the same trip. */
  void reverseDriverPenaltyDeduction(long bookingId, BigDecimal amount);

  /** Which of the three collection paths actually took a fee. */
  enum PenaltyCollection {
    /** Netted out of money already captured; the remainder was refunded. */
    NETTED,
    /** Charged against a live authorisation that had not been captured. */
    CARD_CHARGE,
    /** Nothing to take from. Recorded as dues and carried to her next booking. */
    DUES
  }
}
