package com.routeshare.routing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "route_bucket_cell", schema = "routing")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteBucketCellEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "route_bucket_cell_id")
  private Long id;

  @Column(name = "route_plan_id")
  private Long routePlanId;

  @Column(name = "route_occurrence_id")
  private Long routeOccurrenceId;

  @Column(name = "bucket_resolution")
  private Integer bucketResolution;

  @Column(name = "bucket_cell")
  private String bucketCell;
}
