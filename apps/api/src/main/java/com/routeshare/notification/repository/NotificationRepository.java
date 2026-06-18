package com.routeshare.notification.repository;

import com.routeshare.notification.entity.NotificationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
  List<NotificationEntity> findByAppUserIdOrderByIdDesc(long appUserId, Pageable pageable);

  Optional<NotificationEntity> findByIdAndAppUserId(long id, long appUserId);

  long countByAppUserIdAndReadAtIsNull(long appUserId);
}
