package com.routeshare.finance.repository;

import com.routeshare.finance.entity.FarePolicyEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarePolicyRepository extends JpaRepository<FarePolicyEntity, Long> {
  List<FarePolicyEntity> findAllByOrderByIdDesc();

  java.util.Optional<FarePolicyEntity> findFirstByActiveTrueOrderByIdDesc();
}
