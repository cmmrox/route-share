package com.routeshare.support.repository;

import com.routeshare.support.entity.SupportTicketEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicketEntity, Long> {
  List<SupportTicketEntity> findByAppUserIdOrderByIdDesc(long appUserId);

  Optional<SupportTicketEntity> findByIdAndAppUserId(long id, long appUserId);

  // Admin views (admin module is exempt from cross-module repository access).
  List<SupportTicketEntity> findAllByOrderByIdDesc(Pageable pageable);

  List<SupportTicketEntity> findByStatusOrderByIdDesc(String status, Pageable pageable);
}
