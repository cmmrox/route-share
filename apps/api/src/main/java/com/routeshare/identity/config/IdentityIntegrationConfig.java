package com.routeshare.identity.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({
  NotifyLkProperties.class,
  KeycloakAdminProperties.class,
  OtpDevBypassProperties.class
})
public class IdentityIntegrationConfig {
  @Bean
  RestClient notifyLkRestClient(RestClient.Builder builder) {
    return builder.build();
  }

  @Bean
  RestClient keycloakAdminRestClient(RestClient.Builder builder) {
    return builder.build();
  }
}
