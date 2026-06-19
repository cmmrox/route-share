package com.routeshare.identity.repository;

import com.routeshare.identity.entity.AppUserStatusHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserStatusHistoryRepository
    extends JpaRepository<AppUserStatusHistoryEntity, Long> {
  List<AppUserStatusHistoryEntity> findByAppUserIdOrderByIdDesc(long appUserId);
}
