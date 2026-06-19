package com.routeshare.finance.repository;

import com.routeshare.finance.entity.FinanceAdjustmentEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceAdjustmentRepository extends JpaRepository<FinanceAdjustmentEntity, Long> {
  List<FinanceAdjustmentEntity> findAllByOrderByIdDesc(Pageable pageable);
}
