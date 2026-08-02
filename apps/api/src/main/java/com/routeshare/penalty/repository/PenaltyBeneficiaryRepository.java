package com.routeshare.penalty.repository;

import com.routeshare.penalty.entity.PenaltyBeneficiaryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PenaltyBeneficiaryRepository
    extends JpaRepository<PenaltyBeneficiaryEntity, Long> {

  List<PenaltyBeneficiaryEntity> findByPenaltyId(long penaltyId);

  List<PenaltyBeneficiaryEntity> findByBeneficiaryAppUserIdOrderByIdDesc(long appUserId);
}
