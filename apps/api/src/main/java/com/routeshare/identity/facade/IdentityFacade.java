package com.routeshare.identity.facade;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.identity.domain.AppUser;

public interface IdentityFacade {
  AppUser upsertFromToken(CurrentUser currentUser);
}
