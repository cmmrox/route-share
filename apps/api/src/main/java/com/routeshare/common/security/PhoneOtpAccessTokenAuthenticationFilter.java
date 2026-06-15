package com.routeshare.common.security;

import com.routeshare.identity.service.PhoneOtpAccessTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PhoneOtpAccessTokenAuthenticationFilter extends OncePerRequestFilter {
  private final PhoneOtpAccessTokenService tokens;

  public PhoneOtpAccessTokenAuthenticationFilter(PhoneOtpAccessTokenService tokens) {
    this.tokens = tokens;
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
          new JwtAuthenticationToken(
              jwt, List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")), jwt.getSubject());
      SecurityContextHolder.getContext().setAuthentication(authentication);
      filterChain.doFilter(request, response);
    } catch (BadJwtException ex) {
      SecurityContextHolder.clearContext();
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid phone OTP access token");
    }
  }
}
