package com.routeshare.notification.repository;

import com.routeshare.notification.entity.PushRegistrationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushRegistrationRepository extends JpaRepository<PushRegistrationEntity, Long> {
  List<PushRegistrationEntity> findByAppUserIdAndEnabledTrue(long appUserId);

  Optional<PushRegistrationEntity> findByToken(String token);
}
