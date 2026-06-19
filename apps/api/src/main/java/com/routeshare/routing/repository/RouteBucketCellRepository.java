package com.routeshare.routing.repository;

import com.routeshare.routing.entity.RouteBucketCellEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouteBucketCellRepository extends JpaRepository<RouteBucketCellEntity, Long> {
  @Query(
      value =
          """
      INSERT INTO routing.route_bucket_cell(route_plan_id, route_occurrence_id, bucket_resolution, bucket_cell)
      VALUES (:routePlanId, :routeOccurrenceId, :bucketResolution, :bucketCell)
      ON CONFLICT (route_occurrence_id, bucket_resolution, bucket_cell) DO NOTHING
      RETURNING route_bucket_cell_id
      """,
      nativeQuery = true)
  Long insertCell(
      @Param("routePlanId") long routePlanId,
      @Param("routeOccurrenceId") long routeOccurrenceId,
      @Param("bucketResolution") int bucketResolution,
      @Param("bucketCell") String bucketCell);

  /** Copies a route's bucket-cell set from one occurrence to a newly generated occurrence. */
  @Modifying
  @Query(
      value =
          """
      INSERT INTO routing.route_bucket_cell(route_plan_id, route_occurrence_id, bucket_resolution, bucket_cell)
      SELECT route_plan_id, :targetOccurrenceId, bucket_resolution, bucket_cell
      FROM routing.route_bucket_cell
      WHERE route_occurrence_id = :sourceOccurrenceId
      ON CONFLICT (route_occurrence_id, bucket_resolution, bucket_cell) DO NOTHING
      """,
      nativeQuery = true)
  int copyCellsToOccurrence(
      @Param("sourceOccurrenceId") long sourceOccurrenceId,
      @Param("targetOccurrenceId") long targetOccurrenceId);
}
