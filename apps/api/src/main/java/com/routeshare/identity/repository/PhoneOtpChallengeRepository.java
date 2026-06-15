package com.routeshare.identity.repository;

import com.routeshare.identity.entity.PhoneOtpChallengeEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneOtpChallengeRepository extends JpaRepository<PhoneOtpChallengeEntity, Long> {
  Optional<PhoneOtpChallengeEntity> findByVerificationIdAndPhoneE164(
      UUID verificationId, String phoneE164);

  Optional<PhoneOtpChallengeEntity>
      findFirstByPhoneE164AndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
          String phoneE164, String status, Instant now);
}
