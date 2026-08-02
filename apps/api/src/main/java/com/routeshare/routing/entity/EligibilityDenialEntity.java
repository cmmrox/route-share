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
 * Every refusal, kept — because there is no other trace of one.
 *
 * <p>A rider filtered out of search never made a request, so nothing else in the system knows she
 * wanted the seat. D35 shows the driver what "verified riders only" actually cost him, and that
 * number can only come from here.
 */
@Entity
@Table(name = "eligibility_denial", schema = "routing")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EligibilityDenialEntity {

  public static final String SURFACE_SEARCH = "SEARCH";
  public static final String SURFACE_BOOKING = "BOOKING";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "eligibility_denial_id")
  private Long id;

  @Column(name = "route_occurrence_id", nullable = false)
  private Long routeOccurrenceId;

  @Column(name = "app_user_id", nullable = false)
  private Long appUserId;

  @Column(nullable = false)
  private String reason;

  @Column(nullable = false)
  private String surface;

  @Column(name = "denied_at", insertable = false, updatable = false)
  private Instant deniedAt;

  public static EligibilityDenialEntity of(
      long routeOccurrenceId, long appUserId, String reason, String surface) {
    var e = new EligibilityDenialEntity();
    e.routeOccurrenceId = routeOccurrenceId;
    e.appUserId = appUserId;
    e.reason = reason;
    e.surface = surface;
    return e;
  }
}
