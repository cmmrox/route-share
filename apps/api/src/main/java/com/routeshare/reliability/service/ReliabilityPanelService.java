package com.routeshare.reliability.service;

import com.routeshare.reliability.dto.response.DriverReliabilityResponse;
import com.routeshare.reliability.dto.response.PassengerReliabilityResponse;

/** D28, D34 and P39 — the panels that make a counter answerable rather than merely true. */
public interface ReliabilityPanelService {

  DriverReliabilityResponse driverPanel();

  PassengerReliabilityResponse passengerPanel();

  /**
   * Closes the month that has just ended and opens the next one.
   *
   * <p>Nothing is deleted or zeroed: a new month is a new row, and the prior month stays readable
   * because a rider asking "why am I being asked to prepay?" is asking about last month.
   *
   * @return how many counters were opened for the new month
   */
  int rolloverMonth();
}
