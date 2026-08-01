package com.routeshare.platform.service;

import com.routeshare.platform.dto.response.AppContextResponse;

public interface AppContextService {

  /** Context for the authenticated caller. Never accepts an identifier — own data only. */
  AppContextResponse current();
}
