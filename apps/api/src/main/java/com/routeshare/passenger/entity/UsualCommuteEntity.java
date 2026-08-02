package com.routeshare.passenger.entity;

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
 * P02. The trip a rider makes over and over.
 *
 * <p>Mapped only so the repository has an aggregate root; every read and write goes through native
 * projections, because the two coordinates are PostGIS points and nothing above this layer should
 * have to know that.
 */
@Entity
@Table(name = "usual_commute", schema = "passenger")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsualCommuteEntity {

  @Id
  @Column(name = "app_user_id")
  private Long appUserId;

  @Column(name = "origin_label", nullable = false)
  private String originLabel;

  @Column(name = "destination_label", nullable = false)
  private String destinationLabel;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private Instant updatedAt;
}
