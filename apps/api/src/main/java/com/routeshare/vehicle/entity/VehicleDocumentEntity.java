package com.routeshare.vehicle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicle_document", schema = "vehicle")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VehicleDocumentEntity {
  public static final String STATUS_AWAITING_UPLOAD = "AWAITING_UPLOAD";
  public static final String STATUS_SUBMITTED = "SUBMITTED";
  public static final String STATUS_APPROVED = "APPROVED";
  public static final String STATUS_REJECTED = "REJECTED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "vehicle_document_id")
  private Long id;

  @Column(name = "vehicle_id", nullable = false)
  private Long vehicleId;

  @Column(name = "document_type", nullable = false)
  private String documentType;

  @Column(name = "storage_key", nullable = false)
  private String storageKey;

  @Column(name = "content_type")
  private String contentType;

  @Column(name = "file_size_bytes")
  private Long fileSizeBytes;

  @Column(name = "original_filename")
  private String originalFilename;

  @Column(nullable = false)
  private String status = STATUS_AWAITING_UPLOAD;

  @Column(name = "rejection_reason")
  private String rejectionReason;

  @Column(name = "reviewed_by")
  private Long reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public static VehicleDocumentEntity awaitingUpload(
      long vehicleId,
      String documentType,
      String storageKey,
      String contentType,
      Long fileSizeBytes,
      String originalFilename) {
    var e = new VehicleDocumentEntity();
    e.vehicleId = vehicleId;
    e.documentType = documentType;
    e.storageKey = storageKey;
    e.contentType = contentType;
    e.fileSizeBytes = fileSizeBytes;
    e.originalFilename = originalFilename;
    e.status = STATUS_AWAITING_UPLOAD;
    return e;
  }

  public void markSubmitted(Instant when) {
    this.status = STATUS_SUBMITTED;
    this.submittedAt = when;
    this.rejectionReason = null;
  }
}
