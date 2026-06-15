package com.routeshare.identity.provider;

public interface SmsGateway {
  void sendOtp(String phoneE164, String code, int expiresInMinutes);
}
