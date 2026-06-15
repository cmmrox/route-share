package com.routeshare.identity.provider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.identity.config.NotifyLkProperties;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NotifyLkSmsGatewayTest {
  private static final String PHONE_E164 = "+94" + "771234567";
  private static final String PHONE_NOTIFY = "94" + "771234567";

  @Test
  void sendsOtpSmsToNotifyLkWithoutDemoSenderWhenConfigured() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var gateway =
        new NotifyLkSmsGateway(
            builder.build(),
            new ObjectMapper(),
            new NotifyLkProperties(
                true,
                URI.create("https://app.notify.lk/api/v1"),
                "32043",
                "secret-key",
                "RouteShare",
                false,
                "Your RouteShare verification code is %s. It expires in %d minutes."));

    server
        .expect(
            requestTo(
                "https://app.notify.lk/api/v1/send?user_id=32043&api_key=secret-key&sender_id=RouteShare&to="
                    + PHONE_NOTIFY
                    + "&message=Your%20RouteShare%20verification%20code%20is%20123456.%20It%20expires%20in%205%20minutes."))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess("{\"status\":\"success\",\"data\":\"Sent\"}", MediaType.APPLICATION_JSON));

    gateway.sendOtp(PHONE_E164, "123456", 5);

    server.verify();
  }

  @Test
  void blocksOtpThroughNotifyDemoSenderByDefault() {
    var gateway =
        new NotifyLkSmsGateway(
            RestClient.create(),
            new ObjectMapper(),
            new NotifyLkProperties(
                true,
                URI.create("https://app.notify.lk/api/v1"),
                "32043",
                "secret-key",
                "NotifyDEMO",
                false,
                "Your RouteShare verification code is %s. It expires in %d minutes."));

    assertThatThrownBy(() -> gateway.sendOtp(PHONE_E164, "123456", 5))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("approved Notify.lk sender ID");
  }
}
