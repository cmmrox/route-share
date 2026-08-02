package com.routeshare.routing.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One nameable place in a car, for one occurrence.
 *
 * <p>Per occurrence rather than per plan: a recurring route runs on many days, and Tuesday's front
 * seat being taken says nothing about Wednesday's.
 */
@Entity
@Table(name = "route_occurrence_seat", schema = "routing")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteOccurrenceSeatEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "route_occurrence_seat_id")
  private Long id;

  @Column(name = "route_occurrence_id", nullable = false)
  private Long routeOccurrenceId;

  @Column(name = "slot_index", nullable = false)
  private Integer slotIndex;

  @Column(nullable = false)
  private String label;

  @Column(name = "sub_label", nullable = false)
  private String subLabel;

  public static RouteOccurrenceSeatEntity of(
      long routeOccurrenceId, int slotIndex, String label, String subLabel) {
    var entity = new RouteOccurrenceSeatEntity();
    entity.routeOccurrenceId = routeOccurrenceId;
    entity.slotIndex = slotIndex;
    entity.label = label;
    entity.subLabel = subLabel;
    return entity;
  }
}
