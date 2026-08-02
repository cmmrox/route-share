package com.routeshare.routing.service.impl;

import com.routeshare.common.errors.GateDeniedException;
import com.routeshare.driver.domain.GenderPolicy;
import com.routeshare.passenger.facade.PassengerFacade;
import com.routeshare.routing.domain.EligibilityDecision;
import com.routeshare.routing.entity.EligibilityDenialEntity;
import com.routeshare.routing.entity.RouteOccurrenceEntity;
import com.routeshare.routing.repository.EligibilityDenialRepository;
import com.routeshare.routing.repository.RouteOccurrenceRepository;
import com.routeshare.routing.service.EligibilityService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EligibilityServiceImpl implements EligibilityService {

  private final RouteOccurrenceRepository occurrences;
  private final PassengerFacade passengers;
  private final EligibilityDenialRepository denials;
  private final MeterRegistry meters;

  @Override
  @Transactional(readOnly = true)
  public EligibilityDecision canBook(long appUserId, long routeOccurrenceId) {
    return occurrences
        .findById(routeOccurrenceId)
        .map(occurrence -> decide(appUserId, occurrence))
        .orElseGet(EligibilityDecision::allow);
  }

  @Override
  @Transactional
  public void requireEligible(long appUserId, long routeOccurrenceId) {
    EligibilityDecision decision = canBook(appUserId, routeOccurrenceId);
    if (decision.allowed()) {
      return;
    }
    record(
        routeOccurrenceId, appUserId, decision.reason(), EligibilityDenialEntity.SURFACE_BOOKING);
    // Stated here, unlike in search: she named this trip, so telling her why costs nothing she did
    // not already know, and "no" without a reason is a rider who tries again three more times.
    throw new GateDeniedException(decision.reason(), decision.message(), "/passenger/verification");
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isVerified(long appUserId) {
    return passengers.riderEligibilityProfile(appUserId).isVerified();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isVerifiedFemale(long appUserId) {
    var profile = passengers.riderEligibilityProfile(appUserId);
    return profile.isVerified() && profile.isFemale();
  }

  @Override
  @Transactional
  public void recordSearchDenials(long appUserId, java.util.List<SearchExclusion> exclusions) {
    exclusions.forEach(
        exclusion ->
            record(
                exclusion.routeOccurrenceId(),
                appUserId,
                exclusion.reason(),
                EligibilityDenialEntity.SURFACE_SEARCH));
  }

  private EligibilityDecision decide(long appUserId, RouteOccurrenceEntity occurrence) {
    boolean womenOnly = GenderPolicy.of(occurrence.getGenderPolicy()).isWomenOnly();
    if (!womenOnly && !occurrence.isVerifiedRidersOnly()) {
      return EligibilityDecision.allow();
    }
    var profile = passengers.riderEligibilityProfile(appUserId);
    // Women-only is checked first because it is the stronger claim and subsumes verification: a
    // rider who is not verified cannot be verified-female either, and being told the weaker reason
    // would send her to verify only to be refused again.
    if (womenOnly && !(profile.isVerified() && profile.isFemale())) {
      return EligibilityDecision.denyWomenOnly();
    }
    if (occurrence.isVerifiedRidersOnly() && !profile.isVerified()) {
      return EligibilityDecision.denyVerifiedOnly();
    }
    return EligibilityDecision.allow();
  }

  private void record(long routeOccurrenceId, long appUserId, String reason, String surface) {
    denials.save(EligibilityDenialEntity.of(routeOccurrenceId, appUserId, reason, surface));
    meters.counter("routeshare_eligibility_denials_total", "reason", reason).increment();
  }
}
