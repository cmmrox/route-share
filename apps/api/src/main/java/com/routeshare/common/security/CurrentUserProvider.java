package com.routeshare.common.security;

import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
  public CurrentUser requireCurrentUser() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt))
      throw new IllegalStateException("Authenticated JWT required");
    var roles = new LinkedHashSet<String>();
    auth.getAuthorities()
        .forEach(
            a -> {
              if (a.getAuthority().startsWith("ROLE_")) roles.add(a.getAuthority().substring(5));
            });
    return new CurrentUser(
        jwt.getSubject(),
        jwt.getClaimAsString("email"),
        jwt.getClaimAsString("phone_number"),
        firstNonBlank(jwt.getClaimAsString("name"), jwt.getClaimAsString("preferred_username")),
        roles);
  }

  private String firstNonBlank(String a, String b) {
    return a != null && !a.isBlank() ? a : b;
  }
}
