package com.routeshare.payment.gateway.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class CybersourceSignerTest {
  // Any base64 value works as a test secret; signing is deterministic for fixed inputs.
  private static final String SECRET = Base64.getEncoder().encodeToString("test-secret".getBytes());
  private final CybersourceSigner signer = new CybersourceSigner("key-123", SECRET);

  @Test
  void digestHasSha256PrefixAndIsStable() {
    String d1 = CybersourceSigner.digest("{\"a\":1}");
    String d2 = CybersourceSigner.digest("{\"a\":1}");
    assertThat(d1).startsWith("SHA-256=");
    assertThat(d1).isEqualTo(d2);
    assertThat(CybersourceSigner.digest("{\"a\":2}")).isNotEqualTo(d1);
  }

  @Test
  void postSignatureHeaderIncludesDigestAndCanonicalHeaders() {
    String header =
        signer.signatureHeader(
            "post",
            "/pts/v2/payments",
            "apitest.cybersource.com",
            "Wed, 18 Jun 2026 00:00:00 GMT",
            "merchant-1",
            CybersourceSigner.digest("{}"));
    assertThat(header).contains("keyid=\"key-123\"");
    assertThat(header).contains("algorithm=\"HmacSHA256\"");
    assertThat(header).contains("headers=\"host date (request-target) digest v-c-merchant-id\"");
    assertThat(header).contains("signature=\"");
  }

  @Test
  void getSignatureHeaderOmitsDigest() {
    String header =
        signer.signatureHeader(
            "get",
            "/pts/v2/payments/123",
            "apitest.cybersource.com",
            "Wed, 18 Jun 2026 00:00:00 GMT",
            "merchant-1",
            null);
    assertThat(header).contains("headers=\"host date (request-target) v-c-merchant-id\"");
  }

  @Test
  void hmacIsDeterministic() {
    assertThat(signer.hmacBase64("data")).isEqualTo(signer.hmacBase64("data"));
    assertThat(signer.hmacBase64("data")).isNotEqualTo(signer.hmacBase64("other"));
  }
}
