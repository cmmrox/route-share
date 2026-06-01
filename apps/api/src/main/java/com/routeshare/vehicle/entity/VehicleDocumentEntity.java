package com.routeshare.vehicle.entity;

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
@Table(name = "vehicle_document", schema = "vehicle")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class VehicleDocumentEntity {
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

  @Column(insertable = false)
  private String status;

  @Column(name = "rejection_reason", insertable = false)
  private String rejectionReason;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;
}
