package com.routeshare.identity.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.identity.config.KeycloakAdminProperties;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KeycloakPhoneVerifiedIdentityServiceTest {
  private static final String PHONE = "+94" + "771234567";

  @Test
  void createsPhoneUserAssignsPassengerRoleAndReturnsKeycloakSubject() {
    var fixture = fixture();

    expectAdminToken(fixture.server());
    expectExactUsernameLookup(fixture.server(), "[]");
    fixture
        .server()
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
    expectRoleAssignment(fixture.server(), "kc-user-123");

    var user = fixture.service().ensurePassengerUser(PHONE);

    assertThat(user.subject()).isEqualTo("kc-user-123");
    assertThat(user.phoneNumber()).isEqualTo(PHONE);
    assertThat(user.roles()).containsExactly("PASSENGER");
    fixture.server().verify();
  }

  @Test
  void usesExistingExactUsernameWhenPhoneNumberAlreadyRegistered() {
    var fixture = fixture();

    expectAdminToken(fixture.server());
    expectExactUsernameLookup(
        fixture.server(),
        """
        [{"id":"kc-existing-123","username":"+94771234567"}]
        """);
    expectRoleAssignment(fixture.server(), "kc-existing-123");

    var user = fixture.service().ensurePassengerUser(PHONE);

    assertThat(user.subject()).isEqualTo("kc-existing-123");
    assertThat(user.phoneNumber()).isEqualTo(PHONE);
    fixture.server().verify();
  }

  @Test
  void resolvesCreateConflictOnlyByExactUsernameSoSameNumberCanLoginAgain() {
    var fixture = fixture();

    expectAdminToken(fixture.server());
    expectExactUsernameLookup(fixture.server(), "[]");
    fixture
        .server()
        .expect(requestTo("http://localhost:8081/admin/realms/routeshare/users"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.CONFLICT));
    expectExactUsernameLookup(
        fixture.server(),
        """
        [{"id":"kc-existing-123","username":"+94771234567"}]
        """);
    expectRoleAssignment(fixture.server(), "kc-existing-123");

    var user = fixture.service().ensurePassengerUser(PHONE);

    assertThat(user.subject()).isEqualTo("kc-existing-123");
    assertThat(user.phoneNumber()).isEqualTo(PHONE);
    fixture.server().verify();
  }

  @Test
  void doesNotResolveConflictByAttributeWhenUsernameIsNotThePhoneNumber() {
    var fixture = fixture();

    expectAdminToken(fixture.server());
    expectExactUsernameLookup(fixture.server(), "[]");
    fixture
        .server()
        .expect(requestTo("http://localhost:8081/admin/realms/routeshare/users"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.CONFLICT));
    expectExactUsernameLookup(
        fixture.server(),
        """
        [{"id":"kc-wrong-123","username":"not-the-phone","attributes":{"phone_number":["+94771234567"]}}]
        """);

    assertThatThrownBy(() -> fixture.service().ensurePassengerUser(PHONE))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Keycloak user already exists but username could not be resolved");
    fixture.server().verify();
  }

  private static Fixture fixture() {
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
    return new Fixture(server, service);
  }

  private static void expectAdminToken(MockRestServiceServer server) {
    server
        .expect(requestTo("http://localhost:8081/realms/master/protocol/openid-connect/token"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {"access_token":"admin-token"}
                """,
                MediaType.APPLICATION_JSON));
  }

  private static void expectExactUsernameLookup(MockRestServiceServer server, String response) {
    server
        .expect(
            requestTo(
                "http://localhost:8081/admin/realms/routeshare/users?username=%2B94771234567&exact=true"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
        .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
  }

  private static void expectRoleAssignment(MockRestServiceServer server, String userId) {
    server
        .expect(requestTo("http://localhost:8081/admin/realms/routeshare/roles/PASSENGER"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
        .andRespond(
            withSuccess(
                """
                {"id":"role-1","name":"PASSENGER"}
                """,
                MediaType.APPLICATION_JSON));
    server
        .expect(
            requestTo(
                "http://localhost:8081/admin/realms/routeshare/users/"
                    + userId
                    + "/role-mappings/realm"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
        .andRespond(withSuccess());
  }

  private record Fixture(
      MockRestServiceServer server, KeycloakPhoneVerifiedIdentityServiceImpl service) {}
}
