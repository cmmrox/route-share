package com.routeshare.platform.repository;

import com.routeshare.platform.entity.PolicySettingHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicySettingHistoryRepository
    extends JpaRepository<PolicySettingHistoryEntity, Long> {
  List<PolicySettingHistoryEntity> findByPolicyKeyOrderByIdDesc(String policyKey);
}
