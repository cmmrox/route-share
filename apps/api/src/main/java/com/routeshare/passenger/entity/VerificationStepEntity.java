package com.routeshare.passenger.entity;

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

/** One of P29's four captures, with the attestation the reviewer sees alongside the image. */
@Entity
@Table(name = "verification_step", schema = "passenger")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationStepEntity {

  public static final String PENDING = "PENDING";
  public static final String SUBMITTED = "SUBMITTED";
  public static final String APPROVED = "APPROVED";
  public static final String REJECTED = "REJECTED";

  /** The only capture source the schema admits — POLICY.verifyCameraOnly. */
  public static final String CAMERA = "CAMERA";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "verification_step_id")
  private Long id;

  @Column(name = "session_id", nullable = false)
  private Long sessionId;

  @Column(name = "step_key", nullable = false)
  private String stepKey;

  @Column(name = "document_id")
  private Long documentId;

  @Column(name = "capture_source")
  private String captureSource;

  @Column(name = "captured_at")
  private Instant capturedAt;

  @Column(nullable = false)
  private String status = PENDING;

  @Column(name = "rejection_reason")
  private String rejectionReason;

  public static VerificationStepEntity pending(long sessionId, String stepKey) {
    var e = new VerificationStepEntity();
    e.sessionId = sessionId;
    e.stepKey = stepKey;
    e.status = PENDING;
    return e;
  }

  public void attachCapture(long documentId, String captureSource, Instant capturedAt) {
    this.documentId = documentId;
    this.captureSource = captureSource;
    this.capturedAt = capturedAt;
  }

  public void markSubmitted() {
    this.status = SUBMITTED;
    this.rejectionReason = null;
  }

  public void approve() {
    this.status = APPROVED;
    this.rejectionReason = null;
  }

  public void reject(String reason) {
    this.status = REJECTED;
    this.rejectionReason = reason;
  }
}
