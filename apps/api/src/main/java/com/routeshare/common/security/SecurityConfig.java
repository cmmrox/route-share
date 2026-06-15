package com.routeshare.common.security;

import com.routeshare.identity.service.PhoneOtpAccessTokenService;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, PhoneOtpAccessTokenAuthenticationFilter phoneOtpFilter) throws Exception {
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtRoleConverter());
    var defaultBearerResolver = new DefaultBearerTokenResolver();
    BearerTokenResolver bearerResolver =
        request -> {
          String token = defaultBearerResolver.resolve(request);
          return token != null && token.startsWith(PhoneOtpAccessTokenService.TOKEN_PREFIX)
              ? null
              : token;
        };
    return http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/api/v1/app/config",
                        "/api/v1/auth/otp/request",
                        "/api/v1/auth/otp/verify",
                        "/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth ->
                oauth
                    .bearerTokenResolver(bearerResolver)
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
        .addFilterBefore(phoneOtpFilter, BearerTokenAuthenticationFilter.class)
        .build();
  }
}
