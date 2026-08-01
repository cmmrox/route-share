package com.routeshare.platform.repository;

import com.routeshare.platform.entity.PolicySettingEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicySettingRepository extends JpaRepository<PolicySettingEntity, String> {
  List<PolicySettingEntity> findAllByOrderByPolicyKeyAsc();
}
