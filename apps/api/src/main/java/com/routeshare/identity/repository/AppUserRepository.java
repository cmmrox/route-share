package com.routeshare.identity.repository;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.entity.AppUserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {
  String ACTIVE_STATUS = "ACTIVE";

  Optional<AppUserEntity> findByKeycloakSubject(String keycloakSubject);

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
      throw new AccessDeniedException("User account is not active");
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
}
