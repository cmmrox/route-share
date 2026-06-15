package com.routeshare.identity.service;

import java.util.Set;

public interface PhoneVerifiedIdentityService {
  VerifiedPhoneUser ensurePassengerUser(String phoneE164);

  record VerifiedPhoneUser(
      String subject, String phoneNumber, String displayName, Set<String> roles) {}
}
