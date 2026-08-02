package com.routeshare.routing.entity;

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

/**
 * D14. The short code behind a shared trip.
 *
 * <p>A code is pasted into a WhatsApp group, so it has to be unguessable rather than merely unique:
 * ten base32 characters is about fifty bits, and anything shorter turns the trip table into
 * something a script can walk.
 *
 * <p>Revocation is a timestamp rather than a delete, so "who did I share this with, and when did I
 * stop" survives it. A revoked code answers 404 rather than 410 — 410 confirms the code once
 * existed, which is exactly the signal an enumerator is looking for.
 */
@Entity
@Table(name = "route_occurrence_share", schema = "routing")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteOccurrenceShareEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "route_occurrence_share_id")
  private Long id;

  @Column(name = "route_occurrence_id", nullable = false)
  private Long routeOccurrenceId;

  @Column(name = "short_code", nullable = false)
  private String shortCode;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  public static RouteOccurrenceShareEntity of(long routeOccurrenceId, String shortCode) {
    var e = new RouteOccurrenceShareEntity();
    e.routeOccurrenceId = routeOccurrenceId;
    e.shortCode = shortCode;
    return e;
  }

  public void revoke(Instant when) {
    this.revokedAt = when;
  }

  public boolean isLive() {
    return revokedAt == null;
  }
}
