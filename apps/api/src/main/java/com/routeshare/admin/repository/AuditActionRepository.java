package com.routeshare.admin.repository;

import com.routeshare.admin.entity.AuditActionEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditActionRepository extends JpaRepository<AuditActionEntity, Long> {
  List<AuditActionEntity> findAllByOrderByIdDesc(Pageable pageable);
}
