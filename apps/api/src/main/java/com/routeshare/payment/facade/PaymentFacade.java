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
}
