package com.routeshare.notification.repository;

import com.routeshare.notification.entity.NotificationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
  List<NotificationEntity> findByAppUserIdOrderByIdDesc(long appUserId, Pageable pageable);

  Optional<NotificationEntity> findByIdAndAppUserId(long id, long appUserId);

  long countByAppUserIdAndReadAtIsNull(long appUserId);

  long countByAppUserIdAndReadAtIsNullAndCategoryIn(long appUserId, List<String> categories);

  List<NotificationEntity> findByAppUserIdAndCategoryInOrderByIdDesc(
      long appUserId, List<String> categories, Pageable pageable);

  @Modifying
  @Query(
      "update NotificationEntity n set n.readAt = :now"
          + " where n.appUserId = :appUserId and n.readAt is null")
  int markAllRead(@Param("appUserId") long appUserId, @Param("now") java.time.Instant now);
}
