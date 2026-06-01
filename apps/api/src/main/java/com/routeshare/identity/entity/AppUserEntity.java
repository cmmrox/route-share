package com.routeshare.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_user", schema = "identity")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUserEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "app_user_id")
  private Long id;

  @Column(name = "public_id", insertable = false, updatable = false)
  private UUID publicId;

  @Column(name = "keycloak_subject", nullable = false, unique = true)
  private String keycloakSubject;

  private String email;
  private String phone;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "local_status", insertable = false)
  private String localStatus;
}
