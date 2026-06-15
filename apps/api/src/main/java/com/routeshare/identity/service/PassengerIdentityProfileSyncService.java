package com.routeshare.identity.service;

import java.util.Map;

public interface PassengerIdentityProfileSyncService {
  void syncPassengerProfile(
      String keycloakSubject, String fullName, String photoUrl, Map<String, Object> preferences);
}
