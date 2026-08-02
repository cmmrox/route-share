package com.routeshare.driver.service;

import com.routeshare.driver.dto.request.DrivingPreferenceRequest;
import com.routeshare.driver.dto.response.DrivingPreferenceResponse;
import com.routeshare.driver.dto.response.EligibilityImpactResponse;

/**
 * D35 — the standing answers every trip a driver publishes inherits.
 *
 * <p>Only one of them is gated. Women-only may be set solely by a driver whose own NIC verifies her
 * as female; setting it otherwise is refused with {@code WOMEN_ONLY_NOT_AVAILABLE} rather than
 * quietly ignored, because a preference that silently does not apply is the worst of both.
 */
public interface DrivingPreferenceService {

  /** The current six, creating the row with its defaults on first read. */
  DrivingPreferenceResponse mine();

  DrivingPreferenceResponse update(DrivingPreferenceRequest request);

  /** D35's cost line and the verified share on this driver's own corridors. */
  EligibilityImpactResponse eligibilityImpact();
}
