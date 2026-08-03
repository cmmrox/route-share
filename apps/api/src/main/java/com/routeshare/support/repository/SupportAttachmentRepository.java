package com.routeshare.support.repository;

import com.routeshare.support.entity.SupportAttachmentEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportAttachmentRepository extends JpaRepository<SupportAttachmentEntity, Long> {
  Optional<SupportAttachmentEntity> findByIdAndTicketId(long id, long ticketId);
}
