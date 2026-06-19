package com.routeshare.finance.repository;

import com.routeshare.finance.entity.CommissionRuleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionRuleRepository extends JpaRepository<CommissionRuleEntity, Long> {
  List<CommissionRuleEntity> findAllByOrderByIdDesc();
}
