package com.routeshare.passenger.repository;

import com.routeshare.passenger.entity.PassengerProfileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PassengerProfileRepository extends JpaRepository<PassengerProfileEntity, Long> {
  boolean existsByAppUserId(long appUserId);

  @Query(
      value =
          """
          SELECT passenger_profile_id AS id, full_name AS fullName, photo_url AS photoUrl, preferences::text AS preferencesJson
          FROM passenger.passenger_profile
          WHERE app_user_id = :appUserId
          """,
      nativeQuery = true)
  Optional<PassengerProfileRow> findByAppUserId(@Param("appUserId") long appUserId);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          INSERT INTO passenger.passenger_profile(app_user_id, full_name, photo_url, preferences)
          VALUES (:appUserId, :fullName, :photoUrl, CAST(:preferencesJson AS jsonb))
          ON CONFLICT (app_user_id) DO UPDATE SET full_name = EXCLUDED.full_name,
            photo_url = EXCLUDED.photo_url, preferences = EXCLUDED.preferences, updated_at = now()
          """,
      nativeQuery = true)
  void upsert(
      @Param("appUserId") long appUserId,
      @Param("fullName") String fullName,
      @Param("photoUrl") String photoUrl,
      @Param("preferencesJson") String preferencesJson);

  interface PassengerProfileRow {
    long getId();

    String getFullName();

    String getPhotoUrl();

    String getPreferencesJson();

    default long id() {
      return getId();
    }

    default String fullName() {
      return getFullName();
    }

    default String photoUrl() {
      return getPhotoUrl();
    }

    default String preferencesJson() {
      return getPreferencesJson();
    }
  }
}
