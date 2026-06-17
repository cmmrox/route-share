package com.routeshare.identity.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({NotifyLkProperties.class, KeycloakAdminProperties.class})
public class IdentityIntegrationConfig {
  @Bean
  RestClient notifyLkRestClient(RestClient.Builder builder) {
    return builder.build();
  }

  @Bean
  RestClient keycloakAdminRestClient(RestClient.Builder builder) {
    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(3));
    requestFactory.setReadTimeout(Duration.ofSeconds(3));
    return builder.requestFactory(requestFactory).build();
  }
}
