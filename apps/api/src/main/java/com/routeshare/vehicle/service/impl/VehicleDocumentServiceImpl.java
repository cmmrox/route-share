package com.routeshare.vehicle.service.impl;

import com.routeshare.common.event.DomainEvent;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.storage.config.ObjectStorageProperties;
import com.routeshare.storage.domain.DocumentUploadPolicy;
import com.routeshare.storage.dto.DownloadUrlResponse;
import com.routeshare.storage.dto.UploadUrlRequest;
import com.routeshare.storage.dto.UploadUrlResponse;
import com.routeshare.storage.service.ObjectStoragePort;
import com.routeshare.vehicle.dto.response.VehicleDocumentResponse;
import com.routeshare.vehicle.entity.VehicleDocumentEntity;
import com.routeshare.vehicle.facade.VehicleFacade;
import com.routeshare.vehicle.mapper.VehicleMapper;
import com.routeshare.vehicle.repository.VehicleDocumentRepository;
import com.routeshare.vehicle.service.VehicleDocumentService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class VehicleDocumentServiceImpl implements VehicleDocumentService {
  private static final String SCOPE = "vehicle";

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final DriverFacade driverFacade;
  private final VehicleFacade vehicleFacade;
  private final VehicleDocumentRepository documents;
  private final VehicleMapper vehicleMapper;
  private final ObjectStoragePort storage;
  private final ObjectStorageProperties storageProps;
  private final DomainEventPublisher events;
  private final Clock clock;

  @Override
  @Transactional
  public UploadUrlResponse createUploadUrl(long vehicleId, UploadUrlRequest req) {
    requireOwnedVehicle(vehicleId);
    DocumentUploadPolicy.validate(req.contentType(), req.fileSizeBytes());
    String key =
        DocumentUploadPolicy.storageKey(SCOPE, vehicleId, req.documentType(), req.contentType());
    var saved =
        documents.save(
            VehicleDocumentEntity.awaitingUpload(
                vehicleId,
                req.documentType(),
                key,
                req.contentType(),
                req.fileSizeBytes(),
                req.originalFilename()));
    var ttl = Duration.ofSeconds(storageProps.presignTtlSeconds());
    var presigned = storage.createUploadUrl(key, req.contentType(), ttl);
    return new UploadUrlResponse(
        saved.getId(),
        key,
        presigned.url() == null ? null : presigned.url().toString(),
        presigned.httpMethod(),
        presigned.headers(),
        ttl.toSeconds());
  }

  @Override
  @Transactional
  public VehicleDocumentResponse submit(long vehicleId, long documentId) {
    var doc = requireOwnedDocument(vehicleId, documentId);
    if (!storage.exists(doc.getStorageKey())) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.CONFLICT,
          "Uploaded file was not found in storage. Re-upload before submitting.");
    }
    doc.markSubmitted(Instant.now(clock));
    var response = vehicleMapper.toDocumentResponse(doc);
    events.publish(
        DomainEvent.of(
            "vehicle.document.submitted",
            "vehicle_document",
            String.valueOf(doc.getId()),
            "{\"vehicleId\":" + vehicleId + "}"));
    return response;
  }

  @Override
  @Transactional(readOnly = true)
  public List<VehicleDocumentResponse> listMine(long vehicleId) {
    requireOwnedVehicle(vehicleId);
    return documents.findByVehicleIdOrderByIdDesc(vehicleId).stream()
        .map(vehicleMapper::toDocumentResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public DownloadUrlResponse downloadUrl(long vehicleId, long documentId) {
    var doc = requireOwnedDocument(vehicleId, documentId);
    var ttl = Duration.ofSeconds(storageProps.presignTtlSeconds());
    return new DownloadUrlResponse(
        storage.createDownloadUrl(doc.getStorageKey(), ttl).toString(), ttl.toSeconds());
  }

  private VehicleDocumentEntity requireOwnedDocument(long vehicleId, long documentId) {
    requireOwnedVehicle(vehicleId);
    return documents
        .findByIdAndVehicleId(documentId, vehicleId)
        .orElseThrow(() -> new AccessDeniedException("Document does not belong to this vehicle"));
  }

  private void requireOwnedVehicle(long vehicleId) {
    long driverProfileId = currentDriverProfileId();
    if (!vehicleFacade.existsByVehicleIdAndDriverProfileId(vehicleId, driverProfileId)) {
      throw new AccessDeniedException("Vehicle does not belong to current driver");
    }
  }

  private long currentDriverProfileId() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return driverFacade
        .findDriverProfileIdByAppUserId(app.appUserId())
        .orElseThrow(() -> new AccessDeniedException("Driver profile is required"));
  }
}
