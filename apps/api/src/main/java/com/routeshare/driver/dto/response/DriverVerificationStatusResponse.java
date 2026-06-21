package com.routeshare.driver.dto.response;

import java.util.List;

/**
 * Real driver verification readiness derived from the driver profile, required KYC documents, and
 * vehicle approval — replacing the static workflow_item placeholder.
 */
public record DriverVerificationStatusResponse(
    String profileStatus,
    boolean hasApprovedVehicle,
    boolean ready,
    List<DocumentStatus> documents,
    List<String> nextSteps) {

  public record DocumentStatus(String documentType, String status, String rejectionReason) {}
}
