package com.routeshare.driver.entity;

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
@Table(name = "driver_document", schema = "driver")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DriverDocumentEntity {
  public static final String STATUS_AWAITING_UPLOAD = "AWAITING_UPLOAD";
  public static final String STATUS_SUBMITTED = "SUBMITTED";
  public static final String STATUS_APPROVED = "APPROVED";
  public static final String STATUS_REJECTED = "REJECTED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "driver_document_id")
  private Long id;

  @Column(name = "driver_profile_id", nullable = false)
  private Long driverProfileId;

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

  /** Set at review when the document itself carries an expiry (a licence, an insurance note). */
  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public static DriverDocumentEntity awaitingUpload(
      long driverProfileId,
      String documentType,
      String storageKey,
      String contentType,
      Long fileSizeBytes,
      String originalFilename) {
    var e = new DriverDocumentEntity();
    e.driverProfileId = driverProfileId;
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
