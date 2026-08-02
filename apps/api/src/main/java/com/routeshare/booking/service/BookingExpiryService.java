package com.routeshare.booking.service;

/**
 * D16: a request the driver never answered.
 *
 * <p>Left open it would hold a seat nobody is going to sell and count against the rider's
 * two-request allowance for ever. The lapse is a terminal state of its own — not a cancellation,
 * because neither side chose it, and D16e says so on the screen.
 */
public interface BookingExpiryService {

  /**
   * @return how many lapsed requests this sweep closed
   */
  int sweepExpired(int batchSize);
}
