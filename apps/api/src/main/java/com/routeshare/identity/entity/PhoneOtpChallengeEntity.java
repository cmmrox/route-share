package com.routeshare.identity.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "phone_otp_challenge", schema = "identity")
public class PhoneOtpChallengeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "verification_id", nullable = false, unique = true)
  private UUID verificationId;

  @Column(name = "phone_e164", nullable = false, length = 16)
  private String phoneE164;

  @Column(name = "otp_hash", nullable = false, length = 128)
  private String otpHash;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "resend_available_at", nullable = false)
  private Instant resendAvailableAt;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  protected PhoneOtpChallengeEntity() {}

  public static PhoneOtpChallengeEntity pending(
      UUID verificationId,
      String phoneE164,
      String otpHash,
      Instant createdAt,
      Instant expiresAt,
      Instant resendAvailableAt) {
    var entity = new PhoneOtpChallengeEntity();
    entity.verificationId = verificationId;
    entity.phoneE164 = phoneE164;
    entity.otpHash = otpHash;
    entity.status = "PENDING";
    entity.attempts = 0;
    entity.createdAt = createdAt;
    entity.expiresAt = expiresAt;
    entity.resendAvailableAt = resendAvailableAt;
    return entity;
  }

  public UUID getVerificationId() {
    return verificationId;
  }

  public String getPhoneE164() {
    return phoneE164;
  }

  public String getOtpHash() {
    return otpHash;
  }

  public String getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getResendAvailableAt() {
    return resendAvailableAt;
  }

  public void markVerified(Instant verifiedAt) {
    this.status = "VERIFIED";
    this.verifiedAt = verifiedAt;
  }

  public void markExpired() {
    this.status = "EXPIRED";
  }

  public void registerFailedAttempt() {
    this.attempts += 1;
    if (this.attempts >= 5) {
      this.status = "LOCKED";
    }
  }
}
