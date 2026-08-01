package com.routeshare.driver.service;

import com.routeshare.driver.dto.response.DriverDeactivationResponse;
import com.routeshare.driver.dto.response.DriverReinstatementRequestResponse;
import java.util.List;
import java.util.Optional;

/**
 * Driver deactivation and its way back.
 *
 * <p>Deactivation is not suspension. It stops driving and nothing else: the same person keeps
 * booking rides, and money already earned still pays out. Slice 05's three-missed-starts trigger
 * calls {@link #deactivate} rather than growing its own path.
 */
public interface DriverDeactivationService {
  /** Admin action. Idempotent: a driver already deactivated stays under the original case. */
  DriverDeactivationResponse deactivate(
      long driverProfileId, String reason, String caseRef, long actorAppUserId);

  /** Admin action. Clears the deactivation, restores the role, and closes any open request. */
  DriverDeactivationResponse reinstate(long driverProfileId, long actorAppUserId, String note);

  /** The caller's own open deactivation, for D34. */
  Optional<DriverDeactivationResponse> myActiveDeactivation();

  /** D34's primary action. Refused while an earlier request is still open. */
  DriverReinstatementRequestResponse requestReinstatement(String message);

  List<DriverReinstatementRequestResponse> myReinstatementRequests();
}
