package com.routeshare.reliability.repository;

import com.routeshare.reliability.domain.ReliabilityEventType;
import com.routeshare.reliability.domain.ReliabilityRole;
import com.routeshare.reliability.entity.ReliabilityEventEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReliabilityEventRepository extends JpaRepository<ReliabilityEventEntity, Long> {

  /** D34 renders these: the three misses with their dates, routes and rider counts. */
  List<ReliabilityEventEntity>
      findByAppUserIdAndRoleAndEventTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
          long appUserId,
          ReliabilityRole role,
          ReliabilityEventType eventType,
          Instant from,
          Instant to);

  List<ReliabilityEventEntity> findByAppUserIdAndRoleAndOccurredAtBetweenOrderByOccurredAtDesc(
      long appUserId, ReliabilityRole role, Instant from, Instant to);

  /** The authority a counter is only a cache of; used to rebuild a month. */
  @Query(
      """
      SELECT e.eventType, COUNT(e) FROM ReliabilityEventEntity e
       WHERE e.appUserId = :appUserId AND e.role = :role
         AND e.occurredAt >= :from AND e.occurredAt < :to
       GROUP BY e.eventType
      """)
  List<Object[]> tallyByType(
      @Param("appUserId") long appUserId,
      @Param("role") ReliabilityRole role,
      @Param("from") Instant from,
      @Param("to") Instant to);
}
