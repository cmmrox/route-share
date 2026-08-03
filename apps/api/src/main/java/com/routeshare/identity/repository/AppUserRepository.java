package com.routeshare.identity.repository;

import com.routeshare.common.errors.GateDeniedException;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.entity.AppUserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {
  String ACTIVE_STATUS = "ACTIVE";

  Optional<AppUserEntity> findByKeycloakSubject(String keycloakSubject);

  java.util.List<AppUserEntity> findAllByOrderByIdDesc(org.springframework.data.domain.Pageable p);

  @Query("select u.id from AppUserEntity u where u.localStatus = 'ACTIVE' order by u.id")
  java.util.List<Long> findActiveAppUserIds(org.springframework.data.domain.Pageable pageable);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          INSERT INTO identity.app_user(keycloak_subject, email, phone, display_name)
          VALUES (:subject, :email, :phone, :displayName)
          ON CONFLICT (keycloak_subject)
          DO UPDATE SET email = EXCLUDED.email,
                        phone = EXCLUDED.phone,
                        display_name = EXCLUDED.display_name,
                        updated_at = now()
          """,
      nativeQuery = true)
  void upsertTokenUser(
      @Param("subject") String subject,
      @Param("email") String email,
      @Param("phone") String phone,
      @Param("displayName") String displayName);

  @Transactional
  default AppUser upsertFromToken(CurrentUser user) {
    upsertTokenUser(user.subject(), user.email(), user.phone(), user.displayName());
    AppUser appUser = findBySubject(user.subject()).orElseThrow();
    if (!ACTIVE_STATUS.equals(appUser.localStatus())) {
      // Runs on every request, not only at token mint: a token issued before the suspension must
      // stop working immediately.
      throw GateDeniedException.accountSuspended();
    }
    return appUser;
  }

  default Optional<AppUser> findBySubject(String subject) {
    return findByKeycloakSubject(subject).map(this::toDomain);
  }

  private AppUser toDomain(AppUserEntity entity) {
    return new AppUser(
        entity.getId(),
        entity.getPublicId(),
        entity.getKeycloakSubject(),
        entity.getEmail(),
        entity.getPhone(),
        entity.getDisplayName(),
        entity.getLocalStatus());
  }

  /**
   * First name and dialable number, and nothing else (plan §6.1).
   *
   * <p>A phone-OTP account carries its number as its display name, so a "name" containing digits or
   * an @ is reported as absent rather than echoed back as somebody's first name.
   */
  @Query(
      value =
          """
      SELECT CASE
               WHEN display_name IS NULL THEN NULL
               WHEN display_name ~ '[0-9@]' THEN NULL
               ELSE split_part(btrim(display_name), ' ', 1)
             END AS "firstName",
             phone AS "phone"
        FROM identity.app_user
       WHERE app_user_id = :appUserId
      """,
      nativeQuery = true)
  java.util.Optional<ContactRow> findContactById(
      @org.springframework.data.repository.query.Param("appUserId") long appUserId);

  interface ContactRow {
    String getFirstName();

    String getPhone();
  }
}
