package com.routeshare.location.entity;

import com.routeshare.location.domain.LocationConfidence;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "trip_progress", schema = "location")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripProgressEntity {
  @Id
  @Column(name = "trip_id")
  private Long tripId;

  @Column(name = "route_fraction")
  private BigDecimal routeFraction;

  @Enumerated(EnumType.STRING)
  private LocationConfidence confidence;

  @Column(name = "matched_at")
  private Instant matchedAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "speed_mps")
  private BigDecimal speedMps;

  @Column(name = "bearing_degrees")
  private BigDecimal bearingDegrees;

  @Column(name = "off_route_since")
  private Instant offRouteSince;

  @Column(name = "reversal_candidate_fraction")
  private BigDecimal reversalCandidateFraction;

  @Column(name = "reversal_candidate_count")
  private short reversalCandidateCount;
}
