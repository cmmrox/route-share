package com.routeshare.driver.service;

import com.routeshare.driver.domain.DriverGate;
import java.util.List;

/**
 * Computes why an account cannot drive, or cannot publish, as data.
 *
 * <p>The two questions are kept apart because they fail at different points in a driver's life.
 * <b>Drive gates</b> answer "may this account use driver endpoints at all" — the answer is about
 * the profile. <b>Publish gates</b> answer "may this driver put a route in front of riders" — the
 * answer is about documents, the vehicle and (from slice 02) a rate band, and it can be blocking
 * while every drive gate is clear.
 */
public interface DriverGateService {
  /** Empty when the account may use driver endpoints. */
  List<DriverGate> driveGates(long appUserId);

  /** Drive gates first; publishing blockers only once driving itself is allowed. */
  List<DriverGate> publishGates(long appUserId);

  boolean isDeactivated(long appUserId);
}
