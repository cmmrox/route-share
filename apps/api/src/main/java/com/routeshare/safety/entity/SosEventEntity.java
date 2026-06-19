package com.routeshare.safety.entity;

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
@Table(name = "sos_event", schema = "safety")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SosEventEntity {
  public static final String RAISED = "RAISED";
  public static final String ACKNOWLEDGED = "ACKNOWLEDGED";
  public static final String RESOLVED = "RESOLVED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "sos_event_id")
  private Long id;

  @Column(name = "app_user_id", nullable = false)
  private Long appUserId;

  @Column(name = "owner_role", nullable = false)
  private String ownerRole;

  @Column(name = "trip_id")
  private Long tripId;

  @Column(name = "booking_id")
  private Long bookingId;

  private Double latitude;
  private Double longitude;
  private String note;

  @Column(nullable = false)
  private String status = RAISED;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "resolved_by")
  private Long resolvedBy;

  @Column(name = "resolution_note")
  private String resolutionNote;

  public static SosEventEntity raise(
      long appUserId,
      String ownerRole,
      Long tripId,
      Long bookingId,
      Double latitude,
      Double longitude,
      String note) {
    var e = new SosEventEntity();
    e.appUserId = appUserId;
    e.ownerRole = ownerRole;
    e.tripId = tripId;
    e.bookingId = bookingId;
    e.latitude = latitude;
    e.longitude = longitude;
    e.note = note;
    e.status = RAISED;
    return e;
  }
}
