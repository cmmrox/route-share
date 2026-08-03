package com.routeshare.location.repository;

import com.routeshare.location.entity.RealtimeChannelEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RealtimeChannelRepository extends JpaRepository<RealtimeChannelEntity, Long> {
  @Transactional
  @Modifying
  @Query(
      value =
          """
          INSERT INTO location.realtime_token(token_hash, app_user_id, expires_at)
          VALUES (:hash, :appUserId, :expiresAt)
          """,
      nativeQuery = true)
  int insertToken(
      @Param("hash") String hash,
      @Param("appUserId") long appUserId,
      @Param("expiresAt") Instant expiresAt);

  @Query(
      value =
          """
          SELECT app_user_id
            FROM location.realtime_token
           WHERE token_hash = :hash AND consumed_at IS NULL AND expires_at > :now
          """,
      nativeQuery = true)
  Optional<Long> validTokenOwner(@Param("hash") String hash, @Param("now") Instant now);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          UPDATE location.realtime_token
             SET consumed_at = :now
           WHERE token_hash = :hash AND consumed_at IS NULL AND expires_at > :now
          """,
      nativeQuery = true)
  int consumeToken(@Param("hash") String hash, @Param("now") Instant now);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          INSERT INTO location.realtime_channel(
              app_user_id, connection_id, connected_at, last_seen_at, transport)
          VALUES (:appUserId, :connectionId, :now, :now, :transport)
          ON CONFLICT (connection_id) DO UPDATE SET
              app_user_id = EXCLUDED.app_user_id,
              last_seen_at = EXCLUDED.last_seen_at,
              transport = EXCLUDED.transport
          """,
      nativeQuery = true)
  int register(
      @Param("appUserId") long appUserId,
      @Param("connectionId") String connectionId,
      @Param("transport") String transport,
      @Param("now") Instant now);

  @Transactional
  @Modifying
  @Query(
      value = "DELETE FROM location.realtime_channel WHERE connection_id = :connectionId",
      nativeQuery = true)
  int disconnect(@Param("connectionId") String connectionId);

  boolean existsByAppUserId(long appUserId);

  @Transactional
  @Modifying
  @Query(
      value = "DELETE FROM location.realtime_channel WHERE last_seen_at < :cutoff",
      nativeQuery = true)
  int deleteExpiredChannels(@Param("cutoff") Instant cutoff);

  @Transactional
  @Modifying
  @Query(
      value = "DELETE FROM location.realtime_token WHERE expires_at < :cutoff",
      nativeQuery = true)
  int deleteExpiredTokens(@Param("cutoff") Instant cutoff);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          DELETE FROM location.realtime_channel c
           USING identity.app_user u
           WHERE u.app_user_id = c.app_user_id AND u.status <> 'ACTIVE'
          """,
      nativeQuery = true)
  int deleteIneligibleChannels();
}
