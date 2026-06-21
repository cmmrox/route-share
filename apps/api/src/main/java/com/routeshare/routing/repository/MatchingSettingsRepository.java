package com.routeshare.routing.repository;

import com.routeshare.routing.entity.MatchingSettingsEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchingSettingsRepository extends JpaRepository<MatchingSettingsEntity, Integer> {

  default Optional<MatchingSettingsEntity> current() {
    return findById(MatchingSettingsEntity.SINGLETON_ID);
  }
}
