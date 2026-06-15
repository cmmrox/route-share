package com.routeshare.identity.service.impl;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.identity.config.KeycloakAdminProperties;
import java.net.URI;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KeycloakPassengerIdentityProfileSyncServiceTest {
  @Test
  void updatesKeycloakProfileFieldsAndRouteShareAttributes() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var service =
        new KeycloakPassengerIdentityProfileSyncServiceImpl(
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
        .andRespond(
            withSuccess(
                """
            {"access_token":"admin-token"}
            """,
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("http://localhost:8081/admin/realms/routeshare/users/kc-user-123"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
        .andRespond(
            withSuccess(
                """
                {"enabled":true,"attributes":{"phone_number":["+94700005678"]}}
                """,
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("http://localhost:8081/admin/realms/routeshare/users/kc-user-123"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
        .andExpect(content().string(Matchers.containsString("firstName")))
        .andExpect(content().string(Matchers.containsString("CMMROX")))
        .andExpect(content().string(Matchers.containsString("lastName")))
        .andExpect(content().string(Matchers.containsString("User")))
        .andExpect(content().string(Matchers.containsString("email")))
        .andExpect(content().string(Matchers.containsString("me@example.test")))
        .andExpect(content().string(Matchers.containsString("route_share_full_name")))
        .andExpect(content().string(Matchers.containsString("file:///avatar.jpg")))
        .andRespond(withSuccess());

    service.syncPassengerProfile(
        "kc-user-123",
        "CMMROX User",
        "file:///avatar.jpg",
        Map.of("email", "me@example.test", "referralCode", "REF1"));

    server.verify();
  }
}
