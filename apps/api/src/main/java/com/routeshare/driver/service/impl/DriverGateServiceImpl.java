package com.routeshare.driver.service.impl;

import com.routeshare.common.errors.GateCodes;
import com.routeshare.driver.domain.DriverGate;
import com.routeshare.driver.domain.RequiredDriverDocuments;
import com.routeshare.driver.entity.DriverDocumentEntity;
import com.routeshare.driver.entity.DriverProfileEntity;
import com.routeshare.driver.repository.DriverDeactivationRepository;
import com.routeshare.driver.repository.DriverDocumentRepository;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.driver.service.DriverGateService;
import com.routeshare.vehicle.facade.VehicleFacade;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gate derivation. Every branch here is a screen in the prototype, and the order matters: the first
 * gate is the one the app shows, so it must be the one the user can actually act on next.
 *
 * <p>Deactivation is checked before profile status because the two are independent — a driver
 * deactivated for reliability still has an APPROVED profile, and telling them "you are approved"
 * while refusing every driver call is exactly the confusion D34 exists to prevent.
 */
@Service
@RequiredArgsConstructor
public class DriverGateServiceImpl implements DriverGateService {
  private static final String STATUS_APPROVED = "APPROVED";

  private final DriverProfileRepository drivers;
  private final DriverDeactivationRepository deactivations;
  private final DriverDocumentRepository documents;
  private final VehicleFacade vehicles;
  private final Clock clock;

  @Override
  @Transactional(readOnly = true)
  public List<DriverGate> driveGates(long appUserId) {
    Optional<DriverProfileEntity> profile = drivers.findByAppUserId(appUserId);
    if (profile.isEmpty()) {
      return List.of(
          new DriverGate(
              GateCodes.DRIVER_PROFILE_MISSING,
              "Publish the trips you already make and let riders book the empty seats.",
              "/driver/application"));
    }

    DriverProfileEntity driver = profile.get();
    if (deactivations.existsByDriverProfileIdAndReinstatedAtIsNull(driver.getId())) {
      return List.of(
          new DriverGate(
              GateCodes.DRIVER_DEACTIVATED,
              "Your driver profile is deactivated. You can still ride as a passenger, and any"
                  + " money you've already earned will still be paid out.",
              "/driver/reinstatement-requests"));
    }

    return switch (status(driver)) {
      case STATUS_APPROVED -> List.of();
        // A profile suspended through the admin review path predates the deactivation table; it
        // means
        // the same thing to the driver, so it must read the same way.
      case "SUSPENDED" ->
          List.of(
              new DriverGate(
                  GateCodes.DRIVER_DEACTIVATED,
                  "Your driver profile is deactivated. You can still ride as a passenger, and any"
                      + " money you've already earned will still be paid out.",
                  "/driver/reinstatement-requests"));
      case "REJECTED" ->
          List.of(
              new DriverGate(
                  GateCodes.DRIVER_APPLICATION_REJECTED,
                  "One of your documents needs redoing before you can drive.",
                  "/driver/verification-status"));
      default ->
          List.of(
              new DriverGate(
                  GateCodes.DRIVER_REVIEW_PENDING,
                  "We're checking your documents. Usually done within one working day.",
                  "/driver/verification-status"));
    };
  }

  @Override
  @Transactional(readOnly = true)
  public List<DriverGate> publishGates(long appUserId) {
    List<DriverGate> driveGates = driveGates(appUserId);
    if (!driveGates.isEmpty()) {
      return driveGates;
    }
    // driveGates being empty guarantees an approved profile.
    long driverProfileId = drivers.findByAppUserId(appUserId).orElseThrow().getId();

    List<DriverGate> gates = new ArrayList<>();
    Map<String, DriverDocumentEntity> latestByType = latestDocumentByType(driverProfileId);
    for (String type : RequiredDriverDocuments.TYPES) {
      documentGate(type, latestByType.get(type)).ifPresent(gates::add);
    }
    if (!vehicles.existsApprovedVehicleForDriver(driverProfileId)) {
      gates.add(
          new DriverGate(
              GateCodes.VEHICLE_NOT_APPROVED,
              "Add a vehicle and wait for it to be approved before publishing a route.",
              "/driver/vehicles"));
    } else if (!vehicles.existsPublishableVehicleForDriver(driverProfileId)) {
      // Board D40: approved papers are not a price. A driver whose car is approved but unpriced is
      // told that plainly here, rather than discovering it when publishing fails.
      gates.add(
          new DriverGate(
              GateCodes.RATE_BAND_NOT_SET,
              "ComiGo is still setting the per-km rate for your vehicle. We'll let you know as"
                  + " soon as it's ready.",
              "/driver/vehicles"));
    }
    return List.copyOf(gates);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isDeactivated(long appUserId) {
    return drivers
        .findByAppUserId(appUserId)
        .map(d -> deactivations.existsByDriverProfileIdAndReinstatedAtIsNull(d.getId()))
        .orElse(false);
  }

  /**
   * The document is named by type, never by id, and a rejection reason is not repeated here — the
   * verification screen shows the reviewer's wording, the gate only says which document to fix.
   */
  private Optional<DriverGate> documentGate(String type, DriverDocumentEntity document) {
    String label = type.toLowerCase(Locale.ROOT);
    if (document == null) {
      return Optional.of(
          new DriverGate(
              GateCodes.DOCUMENT_MISSING,
              "Upload your " + label + " document before publishing a route.",
              "/driver/documents"));
    }
    if (DriverDocumentEntity.STATUS_REJECTED.equals(document.getStatus())) {
      return Optional.of(
          new DriverGate(
              GateCodes.DOCUMENT_REJECTED,
              "Your " + label + " document was rejected — upload a valid copy.",
              "/driver/documents"));
    }
    if (!DriverDocumentEntity.STATUS_APPROVED.equals(document.getStatus())) {
      return Optional.of(
          new DriverGate(
              GateCodes.DOCUMENT_MISSING,
              "Finish uploading your " + label + " document and submit it for review.",
              "/driver/documents"));
    }
    if (document.getExpiresAt() != null && document.getExpiresAt().isBefore(clock.instant())) {
      return Optional.of(
          new DriverGate(
              GateCodes.DOCUMENT_EXPIRED,
              "Your " + label + " document has expired — upload a current one.",
              "/driver/documents"));
    }
    return Optional.empty();
  }

  private Map<String, DriverDocumentEntity> latestDocumentByType(long driverProfileId) {
    return documents.findByDriverProfileIdOrderByIdDesc(driverProfileId).stream()
        .collect(
            Collectors.toMap(
                DriverDocumentEntity::getDocumentType,
                d -> d,
                (a, b) -> a.getId() >= b.getId() ? a : b));
  }

  private String status(DriverProfileEntity driver) {
    String status = driver.getVerificationStatus();
    return status == null ? "" : status.toUpperCase(Locale.ROOT);
  }
}
