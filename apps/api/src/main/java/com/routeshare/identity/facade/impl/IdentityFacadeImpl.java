package com.routeshare.identity.facade.impl;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.identity.repository.AppUserRepository;
import com.routeshare.identity.service.KeycloakRealmRoleService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdentityFacadeImpl implements IdentityFacade {
  private final AppUserRepository appUsers;
  private final KeycloakRealmRoleService keycloakRealmRoleService;

  @Override
  public AppUser upsertFromToken(CurrentUser currentUser) {
    return appUsers.upsertFromToken(currentUser);
  }

  @Override
  public void setRealmRoles(String keycloakSubject, Set<String> roles) {
    keycloakRealmRoleService.setRealmRoles(keycloakSubject, roles);
  }
}
