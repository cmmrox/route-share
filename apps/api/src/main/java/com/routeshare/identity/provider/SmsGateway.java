package com.routeshare.identity.provider;

public interface SmsGateway {
  void sendOtp(String phoneE164, String code, int expiresInMinutes);

  /** Sends a free-form transactional SMS (e.g. a trip share-link). */
  void sendText(String phoneE164, String message);
}
