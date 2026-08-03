package com.routeshare.passenger.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.identity.service.PassengerIdentityProfileSyncService;
import com.routeshare.passenger.dto.request.PassengerProfileRequest;
import com.routeshare.passenger.dto.response.PassengerProfileResponse;
import com.routeshare.passenger.mapper.PassengerMapper;
import com.routeshare.passenger.repository.PassengerProfileRepository;
import com.routeshare.passenger.service.PassengerProfileService;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PassengerProfileServiceImpl implements PassengerProfileService {
  private static final Logger log = LoggerFactory.getLogger(PassengerProfileServiceImpl.class);
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final PassengerProfileRepository profiles;
  private final ObjectMapper mapper;
  private final PassengerMapper passengerMapper;
  private final PassengerIdentityProfileSyncService profileSyncService;
  private final com.routeshare.rewards.facade.RewardsFacade rewards;

  @Transactional
  public PassengerProfileResponse upsert(PassengerProfileRequest req) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    var preferences = req.preferences() == null ? Map.<String, Object>of() : req.preferences();
    profiles.upsert(app.appUserId(), req.fullName(), req.photoUrl(), json(preferences));
    rewards.ensureReferralCode(app.appUserId(), req.fullName());
    if (req.referralCode() != null && !req.referralCode().isBlank()) {
      rewards.claimAtSignup(app.appUserId(), app.phone(), req.fullName(), req.referralCode(), null);
    }
    try {
      profileSyncService.syncPassengerProfile(
          app.keycloakSubject(), req.fullName(), req.photoUrl(), preferences);
    } catch (RuntimeException error) {
      log.warn(
          "Passenger profile saved but Keycloak profile sync failed for subject {}",
          app.keycloakSubject(),
          error);
    }
    return get().orElseThrow();
  }

  public Optional<PassengerProfileResponse> get() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return profiles
        .findByAppUserId(app.appUserId())
        .map(
            row ->
                passengerMapper.toPassengerProfileResponse(
                    row, preferences(row.preferencesJson())));
  }

  private String json(Map<String, Object> value) {
    try {
      return mapper.writeValueAsString(value == null ? Map.of() : value);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid passenger preferences", e);
    }
  }

  private Map<String, Object> preferences(String json) {
    try {
      return mapper.readValue(json == null ? "{}" : json, MAP_TYPE);
    } catch (JsonProcessingException e) {
      return Map.of();
    }
  }
}
