package com.routeshare.platform.repository;

import com.routeshare.platform.entity.AccountRequestEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRequestRepository extends JpaRepository<AccountRequestEntity, Long> {
  Optional<AccountRequestEntity> findFirstByAppUserIdAndKindAndStatusInOrderByIdDesc(
      long appUserId, String kind, List<String> statuses);

  List<AccountRequestEntity> findAllByOrderByIdDesc();
}
