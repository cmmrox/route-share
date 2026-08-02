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

  Optional<PassengerProfileEntity> findEntityByAppUserId(long appUserId);

  /**
   * Creates the profile row if a rider has never saved one.
   *
   * <p>Verification, photo visibility and the eligibility inputs all hang off this row, and a rider
   * who signed up by phone OTP and went straight to booking has no other reason to have created it
   * — so a missing row must not mean "not verified and no settings", it must mean "defaults".
   */
  @Transactional
  @Modifying
  @Query(
      value =
          """
          INSERT INTO passenger.passenger_profile(app_user_id, full_name)
          SELECT :appUserId, COALESCE(u.display_name, 'Passenger')
            FROM identity.app_user u
           WHERE u.app_user_id = :appUserId
          ON CONFLICT (app_user_id) DO NOTHING
          """,
      nativeQuery = true)
  void ensureExists(@Param("appUserId") long appUserId);

  /** The three eligibility and disclosure facts, for callers that need nothing else. */
  @Query(
      value =
          """
          SELECT verification_level AS "verificationLevel", gender AS "gender",
                 photo_visibility AS "photoVisibility", photo_url AS "photoUrl"
            FROM passenger.passenger_profile
           WHERE app_user_id = :appUserId
          """,
      nativeQuery = true)
  Optional<RiderProfileRow> findRiderProfile(@Param("appUserId") long appUserId);

  interface RiderProfileRow {
    String getVerificationLevel();

    String getGender();

    String getPhotoVisibility();

    String getPhotoUrl();
  }

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
