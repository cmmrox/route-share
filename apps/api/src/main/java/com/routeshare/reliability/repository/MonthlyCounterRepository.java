package com.routeshare.reliability.repository;

import com.routeshare.reliability.domain.ReliabilityRole;
import com.routeshare.reliability.entity.MonthlyCounterEntity;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyCounterRepository extends JpaRepository<MonthlyCounterEntity, Long> {

  Optional<MonthlyCounterEntity> findByAppUserIdAndRoleAndPeriodMonth(
      long appUserId, ReliabilityRole role, LocalDate periodMonth);

  /** Everyone who had a counter last month — the set the rollover opens fresh rows for. */
  java.util.List<MonthlyCounterEntity> findByPeriodMonth(LocalDate periodMonth);
}
