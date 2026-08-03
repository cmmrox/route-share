package com.routeshare.chat.repository;

import com.routeshare.chat.entity.ChatThreadEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatThreadRepository extends JpaRepository<ChatThreadEntity, Long> {
  Optional<ChatThreadEntity> findByBookingId(long bookingId);

  long countByState(String state);

  List<ChatThreadEntity> findTop200ByStateAndClosesAtLessThanEqualOrderByClosesAt(
      String state, Instant now);
}
