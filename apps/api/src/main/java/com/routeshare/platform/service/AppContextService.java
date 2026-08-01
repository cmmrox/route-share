package com.routeshare.platform.service;

import com.routeshare.platform.dto.response.ActiveModeResponse;
import com.routeshare.platform.dto.response.AppContextResponse;

public interface AppContextService {

  /** Context for the authenticated caller. Never accepts an identifier — own data only. */
  AppContextResponse current();

  /**
   * Persists the mode the user switched into, so the app reopens where they left off.
   *
   * @throws com.routeshare.common.errors.GateConflictException as a 409 when the mode is not
   *     available to this account, carrying the gate code that explains why
   */
  ActiveModeResponse setActiveMode(String mode);
}
