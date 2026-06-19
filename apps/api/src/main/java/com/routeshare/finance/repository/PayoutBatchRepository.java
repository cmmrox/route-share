package com.routeshare.finance.repository;

import com.routeshare.finance.entity.PayoutBatchEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutBatchRepository extends JpaRepository<PayoutBatchEntity, Long> {
  List<PayoutBatchEntity> findAllByOrderByIdDesc(Pageable pageable);

  long countByStatus(String status);
}
