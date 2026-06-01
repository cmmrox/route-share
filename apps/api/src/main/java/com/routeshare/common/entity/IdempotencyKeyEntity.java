package com.routeshare.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "idempotency_key", schema = "common")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKeyEntity {
  @Id
  @Column(name = "idempotency_key")
  private String idempotencyKey;

  @Column(name = "keycloak_subject")
  private String keycloakSubject;

  private String operation;

  @Column(name = "request_hash")
  private String requestHash;

  @Column(name = "response_body", columnDefinition = "jsonb")
  private String responseBody;

  @Column(name = "status_code")
  private Integer statusCode;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "expires_at")
  private Instant expiresAt;
}
