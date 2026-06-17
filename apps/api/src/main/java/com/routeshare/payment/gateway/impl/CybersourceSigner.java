package com.routeshare.payment.gateway.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Implements the Cybersource HTTP Signature scheme: a SHA-256 body digest plus an HMAC-SHA256
 * signature over a canonical set of headers, keyed by the base64-encoded shared secret. Kept as a
 * pure, side-effect-free helper so the crypto can be unit-tested deterministically.
 */
final class CybersourceSigner {
  private final String keyId;
  private final byte[] secretKey;

  CybersourceSigner(String keyId, String sharedSecretBase64) {
    this.keyId = keyId;
    this.secretKey = Base64.getDecoder().decode(sharedSecretBase64);
  }

  /**
   * {@code SHA-256=<base64 sha256(body)>}, the value of the {@code Digest} header for POST bodies.
   */
  static String digest(String body) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(body.getBytes(StandardCharsets.UTF_8));
      return "SHA-256=" + Base64.getEncoder().encodeToString(hash);
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  /**
   * Builds the {@code Signature} header value. For GET requests pass {@code digest = null} so the
   * digest header is omitted from the signed set.
   */
  String signatureHeader(
      String method, String resource, String host, String date, String merchantId, String digest) {
    boolean post = digest != null;
    String headers =
        post
            ? "host date (request-target) digest v-c-merchant-id"
            : "host date (request-target) v-c-merchant-id";
    StringBuilder signingString = new StringBuilder();
    signingString.append("host: ").append(host).append('\n');
    signingString.append("date: ").append(date).append('\n');
    signingString
        .append("(request-target): ")
        .append(method.toLowerCase())
        .append(' ')
        .append(resource)
        .append('\n');
    if (post) {
      signingString.append("digest: ").append(digest).append('\n');
    }
    signingString.append("v-c-merchant-id: ").append(merchantId);

    String signature = hmacBase64(signingString.toString());
    return "keyid=\""
        + keyId
        + "\", algorithm=\"HmacSHA256\", headers=\""
        + headers
        + "\", signature=\""
        + signature
        + "\"";
  }

  String hmacBase64(String data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
      byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(sig);
    } catch (Exception e) {
      throw new IllegalStateException("HMAC-SHA256 signing failed", e);
    }
  }
}
