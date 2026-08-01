package com.routeshare.driver.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.common.security.RouteShareRoles;
import com.routeshare.driver.dto.response.DriverDeactivationResponse;
import com.routeshare.driver.dto.response.DriverReinstatementRequestResponse;
import com.routeshare.driver.entity.DriverDeactivationEntity;
import com.routeshare.driver.entity.DriverProfileEntity;
import com.routeshare.driver.entity.DriverReinstatementRequestEntity;
import com.routeshare.driver.repository.DriverDeactivationRepository;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.driver.repository.DriverReinstatementRequestRepository;
import com.routeshare.driver.service.DriverDeactivationService;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.support.dto.CreateTicketRequest;
import com.routeshare.support.service.SupportService;
import java.time.Clock;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DriverDeactivationServiceImpl implements DriverDeactivationService {
  private static final String TICKET_CATEGORY = "DRIVER_REINSTATEMENT";
  private static final String OWNER_ROLE_DRIVER = "DRIVER";

  private final CurrentUserProvider current;
  private final IdentityFacade identity;
  private final DriverProfileRepository drivers;
  private final DriverDeactivationRepository deactivations;
  private final DriverReinstatementRequestRepository requests;
  private final SupportService support;
  private final Clock clock;

  @Override
  @Transactional
  public DriverDeactivationResponse deactivate(
      long driverProfileId, String reason, String caseRef, long actorAppUserId) {
    DriverProfileEntity driver = requireProfile(driverProfileId);
    Optional<DriverDeactivationEntity> open =
        deactivations.findByDriverProfileIdAndReinstatedAtIsNull(driverProfileId);
    if (open.isPresent()) {
      // Re-issuing a deactivation would rewrite the case reference the driver was already given.
      return toResponse(open.get());
    }
    var saved =
        deactivations.save(
            DriverDeactivationEntity.open(driverProfileId, reason, caseRef, actorAppUserId));
    identity.revokeRealmRole(driver.getAppUserId(), RouteShareRoles.DRIVER);
    return toResponse(saved);
  }

  @Override
  @Transactional
  public DriverDeactivationResponse reinstate(
      long driverProfileId, long actorAppUserId, String note) {
    DriverProfileEntity driver = requireProfile(driverProfileId);
    DriverDeactivationEntity open =
        deactivations
            .findByDriverProfileIdAndReinstatedAtIsNull(driverProfileId)
            .orElseThrow(() -> new IllegalStateException("Driver is not deactivated"));
    open.reinstate(clock.instant(), actorAppUserId);
    deactivations.save(open);

    requests
        .findByDeactivationIdAndStatus(open.getId(), DriverReinstatementRequestEntity.STATUS_OPEN)
        .ifPresent(
            request -> {
              request.decide(
                  DriverReinstatementRequestEntity.STATUS_APPROVED,
                  clock.instant(),
                  actorAppUserId,
                  note);
              requests.save(request);
            });

    identity.grantRealmRole(driver.getAppUserId(), RouteShareRoles.DRIVER);
    return toResponse(open);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<DriverDeactivationResponse> myActiveDeactivation() {
    return deactivations
        .findByDriverProfileIdAndReinstatedAtIsNull(myDriverProfileId())
        .map(this::toResponse);
  }

  @Override
  @Transactional
  public DriverReinstatementRequestResponse requestReinstatement(String message) {
    long driverProfileId = myDriverProfileId();
    DriverDeactivationEntity open =
        deactivations
            .findByDriverProfileIdAndReinstatedAtIsNull(driverProfileId)
            .orElseThrow(() -> new IllegalStateException("Your driver profile is not deactivated"));
    if (requests
        .findByDeactivationIdAndStatus(open.getId(), DriverReinstatementRequestEntity.STATUS_OPEN)
        .isPresent()) {
      throw new IllegalStateException("A reinstatement request is already open");
    }

    // The conversation belongs in support, where an agent already works; the request row is the
    // driver-side state machine that the reinstate action closes.
    var ticket =
        support.create(
            OWNER_ROLE_DRIVER,
            new CreateTicketRequest(
                "Driver reinstatement request — case " + open.getCaseRef(),
                TICKET_CATEGORY,
                "HIGH",
                message));
    var saved =
        requests.save(
            DriverReinstatementRequestEntity.open(
                driverProfileId, open.getId(), message, ticket.id()));
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DriverReinstatementRequestResponse> myReinstatementRequests() {
    return requests.findByDriverProfileIdOrderByIdDesc(myDriverProfileId()).stream()
        .map(this::toResponse)
        .toList();
  }

  private long myDriverProfileId() {
    var app = identity.upsertFromToken(current.requireCurrentUser());
    return drivers
        .findIdByAppUserId(app.appUserId())
        .orElseThrow(() -> new NoSuchElementException("No driver profile"));
  }

  private DriverProfileEntity requireProfile(long driverProfileId) {
    return drivers
        .findById(driverProfileId)
        .orElseThrow(() -> new NoSuchElementException("Driver profile not found"));
  }

  private DriverDeactivationResponse toResponse(DriverDeactivationEntity e) {
    return new DriverDeactivationResponse(
        e.getId(),
        e.getDriverProfileId(),
        e.getReason(),
        e.getCaseRef(),
        e.getDeactivatedAt(),
        e.getReinstatedAt(),
        e.getReinstatedAt() == null);
  }

  private DriverReinstatementRequestResponse toResponse(DriverReinstatementRequestEntity e) {
    return new DriverReinstatementRequestResponse(
        e.getId(),
        e.getDeactivationId(),
        e.getSupportTicketId(),
        e.getMessage(),
        e.getStatus(),
        e.getCreatedAt(),
        e.getDecidedAt(),
        e.getDecisionNote());
  }
}
