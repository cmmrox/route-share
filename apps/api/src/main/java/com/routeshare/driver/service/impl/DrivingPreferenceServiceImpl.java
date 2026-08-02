package com.routeshare.driver.service.impl;

import com.routeshare.common.errors.GateCodes;
import com.routeshare.common.errors.GateDeniedException;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.domain.GenderPolicy;
import com.routeshare.driver.dto.request.DrivingPreferenceRequest;
import com.routeshare.driver.dto.response.DrivingPreferenceResponse;
import com.routeshare.driver.dto.response.EligibilityImpactResponse;
import com.routeshare.driver.entity.DrivingPreferenceEntity;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.driver.repository.DrivingPreferenceRepository;
import com.routeshare.driver.service.DrivingPreferenceService;
import com.routeshare.identity.facade.IdentityFacade;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DrivingPreferenceServiceImpl implements DrivingPreferenceService {

  /** D35 reports the last week, which is the window a driver can still remember. */
  private static final int IMPACT_WINDOW_DAYS = 7;

  private static final String FEMALE = "FEMALE";

  private final CurrentUserProvider current;
  private final IdentityFacade identity;
  private final DriverProfileRepository drivers;
  private final DrivingPreferenceRepository preferences;
  private final Clock clock;

  @Override
  @Transactional
  public DrivingPreferenceResponse mine() {
    long driverProfileId = currentDriverProfileId();
    return toResponse(loadOrCreate(driverProfileId), canSetWomenOnly(driverProfileId));
  }

  @Override
  @Transactional
  public DrivingPreferenceResponse update(DrivingPreferenceRequest request) {
    long driverProfileId = currentDriverProfileId();
    boolean canSetWomenOnly = canSetWomenOnly(driverProfileId);
    GenderPolicy policy = GenderPolicy.of(request.genderPolicy());

    // The first of the two women-only gates. Without it anyone could advertise a women-only car,
    // which is precisely the promise the feature exists to make good on.
    if (policy.isWomenOnly() && !canSetWomenOnly) {
      throw new GateDeniedException(
          GateCodes.WOMEN_ONLY_NOT_AVAILABLE,
          "Women-only trips are offered once your NIC verification is complete and confirms you"
              + " as female.",
          "/driver/verification");
    }

    var entity = loadOrCreate(driverProfileId);
    entity.setGenderPolicy(policy.name());
    entity.setVerifiedRidersOnly(request.verifiedRidersOnly());
    entity.setApproveEachRequest(request.approveEachRequest());
    entity.setMidTripBookings(request.midTripBookings());
    entity.setEarlyDropRequests(request.earlyDropRequests());
    entity.setChatEnabled(request.chatEnabled());
    entity.setUpdatedAt(clock.instant());
    return toResponse(preferences.save(entity), canSetWomenOnly);
  }

  @Override
  @Transactional(readOnly = true)
  public EligibilityImpactResponse eligibilityImpact() {
    long driverProfileId = currentDriverProfileId();
    Instant since = clock.instant().minus(Duration.ofDays(IMPACT_WINDOW_DAYS));
    long turnedAway = preferences.countVerifiedOnlyDenials(driverProfileId, since);
    int share = preferences.verifiedRiderSharePercent(driverProfileId, since);
    return new EligibilityImpactResponse(
        IMPACT_WINDOW_DAYS, turnedAway, share, summary(turnedAway, share));
  }

  private static String summary(long turnedAway, int share) {
    if (turnedAway == 0) {
      return "Verified riders only has turned nobody away this week. "
          + share
          + "% of riders on your routes are verified.";
    }
    return "Verified riders only cost you "
        + turnedAway
        + (turnedAway == 1 ? " request" : " requests")
        + " this week. "
        + share
        + "% of riders on your routes are verified.";
  }

  private DrivingPreferenceEntity loadOrCreate(long driverProfileId) {
    return preferences
        .findById(driverProfileId)
        .orElseGet(
            () ->
                preferences.save(
                    DrivingPreferenceEntity.defaultsFor(driverProfileId, clock.instant())));
  }

  /** D35: the toggle is offered only to a driver her own NIC verifies as female. */
  private boolean canSetWomenOnly(long driverProfileId) {
    return drivers
        .findById(driverProfileId)
        .map(profile -> FEMALE.equalsIgnoreCase(profile.getGender()))
        .orElse(false);
  }

  private long currentDriverProfileId() {
    long appUserId = identity.upsertFromToken(current.requireCurrentUser()).appUserId();
    return drivers
        .findIdByAppUserId(appUserId)
        .orElseThrow(
            () ->
                new GateDeniedException(
                    GateCodes.DRIVER_PROFILE_MISSING,
                    "You haven't applied to drive yet.",
                    "/driver/apply"));
  }

  private static DrivingPreferenceResponse toResponse(
      DrivingPreferenceEntity e, boolean canSetWomenOnly) {
    return new DrivingPreferenceResponse(
        e.getGenderPolicy(),
        e.isVerifiedRidersOnly(),
        e.isApproveEachRequest(),
        e.isMidTripBookings(),
        e.isEarlyDropRequests(),
        e.isChatEnabled(),
        canSetWomenOnly,
        e.getUpdatedAt());
  }
}
