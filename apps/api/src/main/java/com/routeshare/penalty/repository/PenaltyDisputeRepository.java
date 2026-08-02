package com.routeshare.penalty.repository;

import com.routeshare.penalty.entity.PenaltyDisputeEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PenaltyDisputeRepository extends JpaRepository<PenaltyDisputeEntity, Long> {

  @Query(
      """
      SELECT d FROM PenaltyDisputeEntity d
       WHERE d.penaltyId = :penaltyId
       ORDER BY d.id DESC
      """)
  List<PenaltyDisputeEntity> findByPenaltyId(@Param("penaltyId") long penaltyId);

  @Query(
      """
      SELECT d FROM PenaltyDisputeEntity d
       WHERE d.penaltyId = :penaltyId AND d.status = 'OPEN'
      """)
  Optional<PenaltyDisputeEntity> findOpenForPenalty(@Param("penaltyId") long penaltyId);

  @Query(
      """
      SELECT d FROM PenaltyDisputeEntity d
       WHERE (:status IS NULL OR d.status = :status)
       ORDER BY d.raisedAt DESC
      """)
  List<PenaltyDisputeEntity> search(@Param("status") String status);
}
