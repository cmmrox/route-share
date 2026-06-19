package com.routeshare.support.repository;

import com.routeshare.support.entity.SupportMessageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportMessageRepository extends JpaRepository<SupportMessageEntity, Long> {
  List<SupportMessageEntity> findBySupportTicketIdOrderByIdAsc(long supportTicketId);
}
