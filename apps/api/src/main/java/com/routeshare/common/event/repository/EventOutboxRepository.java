package com.routeshare.common.event.repository;

import com.routeshare.common.event.entity.EventOutboxEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface EventOutboxRepository extends JpaRepository<EventOutboxEntity, Long> {

  /**
   * Claims a batch of dispatchable rows for the relay. {@code FOR UPDATE SKIP LOCKED} (via the lock
   * mode + skip-locked hint) lets multiple application instances drain the outbox concurrently
   * without processing the same row twice.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(
      @jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
  @Query(
      "select e from EventOutboxEntity e where e.status = 'PENDING' "
          + "or (e.status = 'FAILED' and e.attempts < :maxAttempts) order by e.id asc")
  List<EventOutboxEntity> claimDispatchable(
      @Param("maxAttempts") int maxAttempts, Pageable pageable);

  boolean existsByIdempotencyKey(String idempotencyKey);
}
