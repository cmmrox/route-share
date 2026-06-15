package com.routeshare.identity.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.identity.config.KeycloakAdminProperties;
import java.net.URI;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KeycloakPhoneVerifiedIdentityServiceTest {
  private static final String PHONE = "+94" + "771234567";

  @Test
  void createsPhoneUserAssignsPassengerRoleAndReturnsKeycloakSubject() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var service =
        new KeycloakPhoneVerifiedIdentityServiceImpl(
            builder.build(),
            new ObjectMapper(),
            new KeycloakAdminProperties(
                true,
                URI.create("http://localhost:8081"),
                "routeshare",
                "master",
                "admin-cli",
                "admin",
                "admin",
                "PASSENGER"));

    server
        .expect(requestTo("http://localhost:8081/realms/master/protocol/openid-connect/token"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("{\"access_token\":\"admin-token\"}", MediaType.APPLICATION_JSON));
    server
        .expect(
            requestTo(Matchers.startsWith("http://localhost:8081/admin/realms/routeshare/users?")))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("http://localhost:8081/admin/realms/routeshare/users"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andRespond(
            request -> {
              var response = withSuccess().createResponse(request);
              response
                  .getHeaders()
                  .setLocation(
                      URI.create(
                          "http://localhost:8081/admin/realms/routeshare/users/kc-user-123"));
              return response;
            });
    server
        .expect(requestTo("http://localhost:8081/admin/realms/routeshare/roles/PASSENGER"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
        .andRespond(
            withSuccess("{\"id\":\"role-1\",\"name\":\"PASSENGER\"}", MediaType.APPLICATION_JSON));
    server
        .expect(
            requestTo(
                "http://localhost:8081/admin/realms/routeshare/users/kc-user-123/role-mappings/realm"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
        .andRespond(withSuccess());

    var user = service.ensurePassengerUser(PHONE);

    assertThat(user.subject()).isEqualTo("kc-user-123");
    assertThat(user.phoneNumber()).isEqualTo(PHONE);
    assertThat(user.roles()).containsExactly("PASSENGER");
    server.verify();
  }
}
