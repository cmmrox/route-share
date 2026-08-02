package com.routeshare.reliability.entity;

import com.routeshare.reliability.domain.ReliabilityEventType;
import com.routeshare.reliability.domain.ReliabilityRole;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The projection of {@link ReliabilityEventEntity} for one user, one role, one calendar month.
 *
 * <p>This is a cache of a count, not the truth. It exists so D28/P39 and the deactivation check do
 * not aggregate the whole event log on every read; it can be rebuilt from the log at any time.
 */
@Entity
@Table(name = "monthly_counter", schema = "reliability")
@Getter
@Setter
@NoArgsConstructor
public class MonthlyCounterEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "monthly_counter_id")
  private Long id;

  @Column(name = "app_user_id", nullable = false)
  private Long appUserId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReliabilityRole role;

  /** Always the first day of the month it counts; a CHECK constraint enforces it. */
  @Column(name = "period_month", nullable = false)
  private LocalDate periodMonth;

  @Column(name = "missed_starts", nullable = false)
  private int missedStarts;

  @Column(name = "late_cancellations", nullable = false)
  private int lateCancellations;

  @Column(name = "start_extensions_used", nullable = false)
  private int startExtensionsUsed;

  @Column(name = "no_shows", nullable = false)
  private int noShows;

  @Column(name = "late_cancels", nullable = false)
  private int lateCancels;

  @Column(name = "early_drops_adjusted", nullable = false)
  private int earlyDropsAdjusted;

  @Column(name = "trips_completed", nullable = false)
  private int tripsCompleted;

  @Column(name = "trips_booked", nullable = false)
  private int tripsBooked;

  @Column(name = "on_time_events", nullable = false)
  private int onTimeEvents;

  @Column(name = "on_time_opportunities", nullable = false)
  private int onTimeOpportunities;

  @Column(name = "updated_at")
  private Instant updatedAt;

  public static MonthlyCounterEntity opened(
      long appUserId, ReliabilityRole role, LocalDate periodMonth) {
    var entity = new MonthlyCounterEntity();
    entity.appUserId = appUserId;
    entity.role = role;
    entity.periodMonth = periodMonth;
    return entity;
  }

  /**
   * Applies one event. Kept here rather than in the service so the mapping from event type to
   * column exists in exactly one place — a projection that disagrees with itself between the live
   * path and a rebuild is worse than no projection.
   */
  public void apply(ReliabilityEventType type) {
    switch (type) {
      case MISSED_START -> missedStarts++;
      case LATE_CANCELLATION -> lateCancellations++;
      case START_EXTENSION_USED -> startExtensionsUsed++;
      case NO_SHOW -> noShows++;
      case LATE_CANCEL -> lateCancels++;
      case EARLY_DROP_ADJUSTED -> earlyDropsAdjusted++;
      case TRIP_COMPLETED -> tripsCompleted++;
      case TRIP_BOOKED -> tripsBooked++;
      case ON_TIME -> onTimeEvents++;
      case ON_TIME_OPPORTUNITY -> onTimeOpportunities++;
        // A correction carries its own delta in the event metadata and is applied by a rebuild, not
        // by guessing a column here.
      case CORRECTION -> {}
    }
  }
}
