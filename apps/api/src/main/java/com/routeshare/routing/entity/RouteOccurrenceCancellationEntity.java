package com.routeshare.routing.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Why a driver called off a published trip, and which window it fell in.
 *
 * <p>The window is stored rather than recomputed. "He cancelled three hours out" is the only thing
 * that explains the fee to the driver who was charged for it, and departure times can be edited
 * before a trip freezes — recomputing later would answer a different question.
 */
@Entity
@Table(name = "route_occurrence_cancellation", schema = "routing")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteOccurrenceCancellationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "route_occurrence_cancellation_id")
  private Long id;

  @Column(name = "route_occurrence_id", nullable = false)
  private Long routeOccurrenceId;

  @Column(name = "cancelled_by_app_user_id", nullable = false)
  private Long cancelledByAppUserId;

  @Column(name = "reason_code", nullable = false)
  private String reasonCode;

  @Column private String note;

  @Column(name = "hours_before_departure", nullable = false)
  private BigDecimal hoursBeforeDeparture;

  @Column(name = "within_free_window", nullable = false)
  private Boolean withinFreeWindow;

  @Column(name = "penalty_id")
  private Long penaltyId;

  @Column(name = "cancelled_at", insertable = false, updatable = false)
  private Instant cancelledAt;

  public static RouteOccurrenceCancellationEntity of(
      long routeOccurrenceId,
      long cancelledByAppUserId,
      String reasonCode,
      String note,
      BigDecimal hoursBeforeDeparture,
      boolean withinFreeWindow) {
    var entity = new RouteOccurrenceCancellationEntity();
    entity.routeOccurrenceId = routeOccurrenceId;
    entity.cancelledByAppUserId = cancelledByAppUserId;
    entity.reasonCode = reasonCode;
    entity.note = note;
    entity.hoursBeforeDeparture = hoursBeforeDeparture;
    entity.withinFreeWindow = withinFreeWindow;
    return entity;
  }
}
