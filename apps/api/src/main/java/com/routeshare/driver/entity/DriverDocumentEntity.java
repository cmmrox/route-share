package com.routeshare.driver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "driver_document", schema = "driver")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DriverDocumentEntity {
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

  @Column(insertable = false)
  private String status;

  @Column(name = "rejection_reason", insertable = false)
  private String rejectionReason;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;
}
