package com.routeshare.passenger.repository;

import com.routeshare.passenger.entity.VerificationStepEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationStepRepository extends JpaRepository<VerificationStepEntity, Long> {

  List<VerificationStepEntity> findBySessionIdOrderByIdAsc(long sessionId);

  Optional<VerificationStepEntity> findBySessionIdAndStepKey(long sessionId, String stepKey);
}
