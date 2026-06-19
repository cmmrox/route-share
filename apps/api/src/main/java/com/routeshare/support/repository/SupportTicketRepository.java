package com.routeshare.support.repository;

import com.routeshare.support.entity.SupportTicketEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicketEntity, Long> {
  List<SupportTicketEntity> findByAppUserIdOrderByIdDesc(long appUserId);

  Optional<SupportTicketEntity> findByIdAndAppUserId(long id, long appUserId);
}
