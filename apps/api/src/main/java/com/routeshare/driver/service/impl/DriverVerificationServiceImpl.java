package com.routeshare.driver.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.domain.RequiredDriverDocuments;
import com.routeshare.driver.dto.response.DriverVerificationStatusResponse;
import com.routeshare.driver.dto.response.DriverVerificationStatusResponse.DocumentStatus;
import com.routeshare.driver.entity.DriverDocumentEntity;
import com.routeshare.driver.repository.DriverDocumentRepository;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.driver.service.DriverVerificationService;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.vehicle.facade.VehicleFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DriverVerificationServiceImpl implements DriverVerificationService {
  private static final String NOT_STARTED = "NOT_STARTED";
  private static final String MISSING = "MISSING";
  private static final String APPROVED = "APPROVED";

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final DriverProfileRepository drivers;
  private final DriverDocumentRepository documents;
  private final VehicleFacade vehicleFacade;

  @Override
  @Transactional(readOnly = true)
  public DriverVerificationStatusResponse status() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    Optional<Long> driverProfileId = drivers.findIdByAppUserId(app.appUserId());
    if (driverProfileId.isEmpty()) {
      List<DocumentStatus> missing =
          RequiredDriverDocuments.TYPES.stream()
              .map(type -> new DocumentStatus(type, MISSING, null))
              .toList();
      return new DriverVerificationStatusResponse(
          NOT_STARTED,
          false,
          false,
          missing,
          List.of("Submit your driver application to begin verification."));
    }

    long profileId = driverProfileId.get();
    String profileStatus = drivers.findStatusByAppUserId(app.appUserId()).orElse(NOT_STARTED);
    Map<String, DriverDocumentEntity> latestByType = latestDocumentByType(profileId);

    List<DocumentStatus> docStatuses = new ArrayList<>();
    List<String> nextSteps = new ArrayList<>();
    boolean allDocsApproved = true;
    for (String type : RequiredDriverDocuments.TYPES) {
      DriverDocumentEntity doc = latestByType.get(type);
      String status = doc == null ? MISSING : doc.getStatus();
      docStatuses.add(
          new DocumentStatus(type, status, doc == null ? null : doc.getRejectionReason()));
      if (!APPROVED.equals(status)) {
        allDocsApproved = false;
        nextSteps.add(nextStepFor(type, status));
      }
    }

    boolean hasApprovedVehicle = vehicleFacade.existsApprovedVehicleForDriver(profileId);
    if (!hasApprovedVehicle) {
      nextSteps.add("Add a vehicle and wait for it to be approved.");
    }

    boolean ready = APPROVED.equals(profileStatus) && allDocsApproved && hasApprovedVehicle;
    if (ready) {
      nextSteps.add("You are verified and ready to publish routes.");
    } else if (allDocsApproved && hasApprovedVehicle && !APPROVED.equals(profileStatus)) {
      nextSteps.add("All documents and vehicle approved — awaiting final account review.");
    }

    return new DriverVerificationStatusResponse(
        profileStatus, hasApprovedVehicle, ready, docStatuses, nextSteps);
  }

  private Map<String, DriverDocumentEntity> latestDocumentByType(long driverProfileId) {
    return documents.findByDriverProfileIdOrderByIdDesc(driverProfileId).stream()
        .collect(
            Collectors.toMap(
                DriverDocumentEntity::getDocumentType,
                d -> d,
                (a, b) -> a.getId() >= b.getId() ? a : b));
  }

  private String nextStepFor(String type, String status) {
    String label = type.toLowerCase(java.util.Locale.ROOT);
    return switch (status) {
      case MISSING -> "Upload your " + label + " document.";
      case DriverDocumentEntity.STATUS_AWAITING_UPLOAD ->
          "Finish uploading your " + label + " document and submit it.";
      case DriverDocumentEntity.STATUS_REJECTED ->
          "Your " + label + " document was rejected — re-upload a valid copy.";
      default -> "Your " + label + " document is awaiting review.";
    };
  }
}
