package com.routeshare.admin.service.impl;

import com.routeshare.admin.dto.AdminDocReviewRequest;
import com.routeshare.admin.dto.AdminDocumentResponse;
import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.admin.service.AdminDocumentService;
import com.routeshare.common.event.DomainEvent;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.entity.DriverDocumentEntity;
import com.routeshare.driver.repository.DriverDocumentRepository;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.passenger.entity.PassengerDocumentEntity;
import com.routeshare.passenger.repository.PassengerDocumentRepository;
import com.routeshare.storage.config.ObjectStorageProperties;
import com.routeshare.storage.dto.DownloadUrlResponse;
import com.routeshare.storage.service.ObjectStoragePort;
import com.routeshare.vehicle.entity.VehicleDocumentEntity;
import com.routeshare.vehicle.repository.VehicleDocumentRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDocumentServiceImpl implements AdminDocumentService {
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final DriverDocumentRepository driverDocs;
  private final VehicleDocumentRepository vehicleDocs;
  private final PassengerDocumentRepository passengerDocs;
  private final ObjectStoragePort storage;
  private final ObjectStorageProperties storageProps;
  private final DomainEventPublisher events;
  private final AdminAuditService audit;

  @Override
  @Transactional
  public AdminDocumentResponse reviewDriverDocument(long documentId, AdminDocReviewRequest req) {
    var doc =
        driverDocs
            .findById(documentId)
            .orElseThrow(() -> new NoSuchElementException("Driver document not found"));
    boolean approve = isApprove(req);
    doc.setStatus(
        approve ? DriverDocumentEntity.STATUS_APPROVED : DriverDocumentEntity.STATUS_REJECTED);
    doc.setRejectionReason(approve ? null : req.rejectionReason());
    doc.setReviewedBy(currentAdminId());
    doc.setReviewedAt(Instant.now());
    afterReview("driver_document", documentId, approve);
    return new AdminDocumentResponse(
        doc.getId(),
        "DRIVER",
        doc.getDocumentType(),
        doc.getStatus(),
        doc.getRejectionReason(),
        doc.getReviewedAt());
  }

  @Override
  @Transactional
  public AdminDocumentResponse reviewVehicleDocument(long documentId, AdminDocReviewRequest req) {
    var doc =
        vehicleDocs
            .findById(documentId)
            .orElseThrow(() -> new NoSuchElementException("Vehicle document not found"));
    boolean approve = isApprove(req);
    doc.setStatus(
        approve ? VehicleDocumentEntity.STATUS_APPROVED : VehicleDocumentEntity.STATUS_REJECTED);
    doc.setRejectionReason(approve ? null : req.rejectionReason());
    doc.setReviewedBy(currentAdminId());
    doc.setReviewedAt(Instant.now());
    afterReview("vehicle_document", documentId, approve);
    return new AdminDocumentResponse(
        doc.getId(),
        "VEHICLE",
        doc.getDocumentType(),
        doc.getStatus(),
        doc.getRejectionReason(),
        doc.getReviewedAt());
  }

  @Override
  @Transactional
  public AdminDocumentResponse reviewPassengerDocument(long documentId, AdminDocReviewRequest req) {
    var doc =
        passengerDocs
            .findById(documentId)
            .orElseThrow(() -> new NoSuchElementException("Passenger document not found"));
    boolean approve = isApprove(req);
    doc.setStatus(
        approve
            ? PassengerDocumentEntity.STATUS_APPROVED
            : PassengerDocumentEntity.STATUS_REJECTED);
    doc.setRejectionReason(approve ? null : req.rejectionReason());
    doc.setReviewedBy(currentAdminId());
    doc.setReviewedAt(Instant.now());
    afterReview("passenger_document", documentId, approve);
    return new AdminDocumentResponse(
        doc.getId(),
        "PASSENGER",
        doc.getDocumentType(),
        doc.getStatus(),
        doc.getRejectionReason(),
        doc.getReviewedAt());
  }

  @Override
  @Transactional(readOnly = true)
  public DownloadUrlResponse driverDocumentDownloadUrl(long documentId) {
    var doc =
        driverDocs
            .findById(documentId)
            .orElseThrow(() -> new NoSuchElementException("Driver document not found"));
    return download(doc.getStorageKey());
  }

  @Override
  @Transactional(readOnly = true)
  public DownloadUrlResponse vehicleDocumentDownloadUrl(long documentId) {
    var doc =
        vehicleDocs
            .findById(documentId)
            .orElseThrow(() -> new NoSuchElementException("Vehicle document not found"));
    return download(doc.getStorageKey());
  }

  @Override
  @Transactional(readOnly = true)
  public DownloadUrlResponse passengerDocumentDownloadUrl(long documentId) {
    var doc =
        passengerDocs
            .findById(documentId)
            .orElseThrow(() -> new NoSuchElementException("Passenger document not found"));
    return download(doc.getStorageKey());
  }

  private boolean isApprove(AdminDocReviewRequest req) {
    if (req == null || req.decision() == null) {
      throw new IllegalArgumentException("decision (APPROVE|REJECT) is required");
    }
    return "APPROVE".equals(req.decision());
  }

  private void afterReview(String targetType, long documentId, boolean approve) {
    String action = approve ? "DOCUMENT_APPROVED" : "DOCUMENT_REJECTED";
    audit.record(action, targetType, String.valueOf(documentId), null);
    events.publish(
        DomainEvent.of(
            "document.reviewed",
            targetType,
            String.valueOf(documentId),
            "{\"approved\":" + approve + "}"));
  }

  private DownloadUrlResponse download(String storageKey) {
    var ttl = Duration.ofSeconds(storageProps.presignTtlSeconds());
    return new DownloadUrlResponse(
        storage.createDownloadUrl(storageKey, ttl).toString(), ttl.toSeconds());
  }

  private long currentAdminId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }
}
