package com.routeshare.driver.repository;

import com.routeshare.driver.dto.response.DriverProfileResponse;
import com.routeshare.driver.entity.DriverProfileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DriverProfileRepository extends JpaRepository<DriverProfileEntity, Long> {
  Optional<DriverProfileEntity> findByAppUserId(long appUserId);

  Optional<DriverProfileEntity> findByAppUserIdAndVerificationStatus(
      long appUserId, String verificationStatus);

  @Transactional
  @Modifying
  @Query(
      value =
          """
      INSERT INTO driver.driver_profile(app_user_id, display_name, verification_status)
      VALUES (:appUserId, :displayName, 'SUBMITTED')
      ON CONFLICT (app_user_id) DO UPDATE SET display_name = EXCLUDED.display_name,
        verification_status = 'SUBMITTED', updated_at = now()
      """,
      nativeQuery = true)
  void submit(@Param("appUserId") long appUserId, @Param("displayName") String displayName);

  default Optional<DriverProfileResponse> findResponseByAppUserId(long appUserId) {
    return findByAppUserId(appUserId).map(this::toResponse);
  }

  default Optional<Long> findIdByAppUserId(long appUserId) {
    return findByAppUserId(appUserId).map(DriverProfileEntity::getId);
  }

  default Optional<Long> findApprovedIdByAppUserId(long appUserId) {
    return findByAppUserIdAndVerificationStatus(appUserId, "APPROVED")
        .map(DriverProfileEntity::getId);
  }

  default Optional<String> findStatusByAppUserId(long appUserId) {
    return findByAppUserId(appUserId).map(DriverProfileEntity::getVerificationStatus);
  }

  default DriverProfileResponse review(long driverProfileId, String status) {
    DriverProfileEntity entity = findById(driverProfileId).orElseThrow();
    entity.setVerificationStatus(status);
    return toResponse(save(entity));
  }

  private DriverProfileResponse toResponse(DriverProfileEntity entity) {
    return new DriverProfileResponse(
        entity.getId(), entity.getDisplayName(), entity.getVerificationStatus());
  }
}
