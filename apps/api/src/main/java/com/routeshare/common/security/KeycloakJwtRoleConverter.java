package com.routeshare.common.security;

import java.util.*;
import java.util.stream.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakJwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Set<String> roles = new LinkedHashSet<>();
    Object realm = jwt.getClaims().get("realm_access");
    if (realm instanceof Map<?, ?> m && m.get("roles") instanceof Collection<?> c)
      c.forEach(r -> roles.add(String.valueOf(r)));
    Object resources = jwt.getClaims().get("resource_access");
    if (resources instanceof Map<?, ?> rm
        && rm.get("api-monolith") instanceof Map<?, ?> api
        && api.get("roles") instanceof Collection<?> c)
      c.forEach(r -> roles.add(String.valueOf(r)));
    return roles.stream()
        .filter(r -> !r.isBlank())
        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
