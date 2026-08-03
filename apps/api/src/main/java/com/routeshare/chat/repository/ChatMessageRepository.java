package com.routeshare.chat.repository;

import com.routeshare.chat.entity.ChatMessageEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
  Optional<ChatMessageEntity> findByThreadIdAndSenderAppUserIdAndIdempotencyKey(
      long threadId, long senderAppUserId, String idempotencyKey);

  List<ChatMessageEntity> findByThreadIdAndIdGreaterThanOrderById(
      long threadId, long afterId, Pageable pageable);

  @Modifying
  @Query(
      """
      UPDATE ChatMessageEntity m
         SET m.readByCounterpartyAt = :now
       WHERE m.threadId = :threadId
         AND m.id <= :upToMessageId
         AND m.senderAppUserId <> :readerId
         AND m.readByCounterpartyAt IS NULL
      """)
  int markRead(
      @Param("threadId") long threadId,
      @Param("readerId") long readerId,
      @Param("upToMessageId") long upToMessageId,
      @Param("now") Instant now);
}
