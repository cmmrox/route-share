package com.routeshare.passenger.repository;

import com.routeshare.passenger.entity.VerificationSessionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationSessionRepository
    extends JpaRepository<VerificationSessionEntity, Long> {

  /** The one live attempt, if there is one — the partial unique index guarantees at most one. */
  @Query(
      value =
          """
      SELECT * FROM passenger.verification_session
       WHERE app_user_id = :appUserId AND status IN ('OPEN', 'SUBMITTED')
       LIMIT 1
      """,
      nativeQuery = true)
  Optional<VerificationSessionEntity> findLive(@Param("appUserId") long appUserId);

  /** The attempt a rider's status screen reports: the live one, or the last one decided. */
  @Query(
      value =
          """
      SELECT * FROM passenger.verification_session
       WHERE app_user_id = :appUserId
       ORDER BY verification_session_id DESC
       LIMIT 1
      """,
      nativeQuery = true)
  Optional<VerificationSessionEntity> findLatest(@Param("appUserId") long appUserId);

  Optional<VerificationSessionEntity> findByIdAndAppUserId(long id, long appUserId);

  /** The reviewer's queue: everything submitted, oldest first. */
  List<VerificationSessionEntity> findByStatusOrderByIdAsc(String status);

  /**
   * How many submissions have been waiting longer than the review SLA. Gauged rather than alerted
   * on a total, because a large queue that is moving is fine and a small one that is not is not.
   */
  @Query(
      value =
          """
      SELECT COUNT(*) FROM passenger.verification_session
       WHERE status = 'SUBMITTED' AND submitted_at < :threshold
      """,
      nativeQuery = true)
  long countPendingOlderThan(@Param("threshold") java.time.Instant threshold);
}
