package com.routeshare.payment.service;

import java.util.List;

/**
 * Finds money that is stuck.
 *
 * <p>Two shapes matter, and both are invisible to the people they affect. An authorisation that is
 * never captured and never voided holds a rider's money for as long as their bank allows — usually
 * a week. An attempt that started and never finished is a gateway call whose outcome nobody knows,
 * which is the one state a retry must not guess at.
 */
public interface PaymentReconciliationService {
  List<StuckPayment> findStuck();

  /**
   * @param kind STUCK_AUTHORIZATION or UNFINISHED_ATTEMPT
   */
  record StuckPayment(
      String kind,
      Long paymentIntentId,
      Long bookingId,
      String status,
      java.math.BigDecimal amount,
      java.time.Instant since,
      String providerReference) {}
}
