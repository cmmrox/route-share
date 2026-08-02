package com.routeshare.trip.service;

/**
 * Turns the driver's location trail into pickup waits.
 *
 * <p>Called on every accepted location sample rather than swept on a tick: the passenger is told
 * "your driver is here" from this, and a minute of latency on that message is a minute she spends
 * indoors while her wait is already running.
 */
public interface ArrivalDetectionService {

  /**
   * Detects arrivals at any pickup on this trip that is still waiting for one.
   *
   * @return how many waits this sample started, which is normally zero.
   */
  int onDriverLocation(long tripId);
}
