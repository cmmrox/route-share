package com.routeshare.common.security;

import com.routeshare.identity.service.AccountRoleService;
import com.routeshare.identity.service.PhoneOtpAccessTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates the phone-OTP token and gives it the account's <em>real</em> authorities.
 *
 * <p>This filter used to stamp {@code ROLE_PASSENGER} on every phone-OTP session. With two apps
 * that was harmless. With one app it meant a phone-OTP user could never drive, no matter how
 * approved their driver profile was — the single hardest blocker in the unified-app plan.
 *
 * <p>Roles are resolved per request from the identity projection (cached briefly, invalidated on
 * every grant, revoke and deactivation), so the two token issuers end up with the same authorities
 * for the same person and a role taken away stops working immediately.
 */
@Component
public class PhoneOtpAccessTokenAuthenticationFilter extends OncePerRequestFilter {
  private final PhoneOtpAccessTokenService tokens;
  private final AccountRoleService accountRoles;

  public PhoneOtpAccessTokenAuthenticationFilter(
      PhoneOtpAccessTokenService tokens, AccountRoleService accountRoles) {
    this.tokens = tokens;
    this.accountRoles = accountRoles;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorization = request.getHeader("Authorization");
    if (authorization == null
        || !authorization.startsWith("Bearer " + PhoneOtpAccessTokenService.TOKEN_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      String tokenValue = authorization.substring("Bearer ".length());
      var jwt = tokens.parse(tokenValue);
      AbstractAuthenticationToken authentication =
          new JwtAuthenticationToken(jwt, authorities(jwt.getSubject()), jwt.getSubject());
      SecurityContextHolder.getContext().setAuthentication(authentication);
      filterChain.doFilter(request, response);
    } catch (BadJwtException ex) {
      SecurityContextHolder.clearContext();
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid phone OTP access token");
    }
  }

  private List<GrantedAuthority> authorities(String subject) {
    return accountRoles.effectiveRoles(subject).stream()
        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
        .toList();
  }
}
