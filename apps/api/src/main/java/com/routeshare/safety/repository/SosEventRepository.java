package com.routeshare.safety.repository;

import com.routeshare.safety.entity.SosEventEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SosEventRepository extends JpaRepository<SosEventEntity, Long> {
  List<SosEventEntity> findByAppUserIdOrderByIdDesc(long appUserId);

  List<SosEventEntity> findByStatusOrderByIdDesc(String status, Pageable pageable);

  Optional<SosEventEntity> findByIdAndAppUserId(long id, long appUserId);
}
