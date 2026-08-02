package com.routeshare.driver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * D35 — the six standing answers a driver gives once, which every trip he publishes then inherits.
 *
 * <p>The row is created on first read with the defaults the schema states, so a driver who has
 * never opened the screen still has a coherent answer to "does this trip need approval?" — and the
 * answer is the cautious one.
 */
@Entity
@Table(name = "driving_preference", schema = "driver")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DrivingPreferenceEntity {

  @Id
  @Column(name = "driver_profile_id")
  private Long driverProfileId;

  @Column(name = "gender_policy", nullable = false)
  private String genderPolicy = "ANYONE";

  @Column(name = "verified_riders_only", nullable = false)
  private boolean verifiedRidersOnly;

  @Column(name = "approve_each_request", nullable = false)
  private boolean approveEachRequest = true;

  @Column(name = "mid_trip_bookings", nullable = false)
  private boolean midTripBookings = true;

  @Column(name = "early_drop_requests", nullable = false)
  private boolean earlyDropRequests = true;

  @Column(name = "chat_enabled", nullable = false)
  private boolean chatEnabled = true;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.EPOCH;

  public static DrivingPreferenceEntity defaultsFor(long driverProfileId, Instant now) {
    var e = new DrivingPreferenceEntity();
    e.driverProfileId = driverProfileId;
    e.updatedAt = now;
    return e;
  }
}
