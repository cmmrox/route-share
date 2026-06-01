package com.routeshare.common.repository;

import com.routeshare.common.entity.IdempotencyKeyEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, String> {
  @Query(
      value =
          """
      SELECT request_hash AS requestHash,
             response_body::text AS responseBody,
             status_code AS statusCode
      FROM common.idempotency_key
      WHERE idempotency_key = :key
        AND keycloak_subject = :keycloakSubject
        AND operation = :operation
        AND expires_at > now()
      """,
      nativeQuery = true)
  Optional<StoredResponse> findActive(
      @Param("key") String key,
      @Param("keycloakSubject") String keycloakSubject,
      @Param("operation") String operation);

  @Query(
      value =
          """
      INSERT INTO common.idempotency_key(
        idempotency_key, keycloak_subject, operation, request_hash, expires_at)
      VALUES (:key, :keycloakSubject, :operation, :requestHash, now() + interval '24 hours')
      ON CONFLICT (idempotency_key) DO NOTHING
      RETURNING idempotency_key
      """,
      nativeQuery = true)
  Optional<String> insertNewReservation(
      @Param("key") String key,
      @Param("keycloakSubject") String keycloakSubject,
      @Param("operation") String operation,
      @Param("requestHash") String requestHash);

  default String reserveNew(
      String key, String keycloakSubject, String operation, String requestHash) {
    return insertNewReservation(key, keycloakSubject, operation, requestHash).orElse(null);
  }

  @Modifying
  @Query(
      value =
          """
      UPDATE common.idempotency_key
      SET response_body = CAST(:responseBody AS jsonb),
          status_code = :statusCode
      WHERE idempotency_key = :key
      """,
      nativeQuery = true)
  void storeResponse(
      @Param("key") String key,
      @Param("responseBody") String responseBody,
      @Param("statusCode") int statusCode);

  interface StoredResponse {
    String getRequestHash();

    String getResponseBody();

    Integer getStatusCode();
  }
}
