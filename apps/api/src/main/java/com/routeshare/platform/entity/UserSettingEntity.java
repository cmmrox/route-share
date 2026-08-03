package com.routeshare.platform.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_setting", schema = "platform")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSettingEntity {
  @Id
  @Column(name = "app_user_id")
  private Long appUserId;

  @Column(nullable = false)
  private String theme = "SYSTEM";

  @Column(nullable = false)
  private String language = "en";

  @Column(name = "share_live_location", nullable = false)
  private boolean shareLiveLocation = true;

  @Column(name = "show_rating_publicly", nullable = false)
  private boolean showRatingPublicly = true;

  @Column(name = "receipts_by_email", nullable = false)
  private boolean receiptsByEmail = true;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public static UserSettingEntity defaultsFor(long appUserId) {
    var settings = new UserSettingEntity();
    settings.appUserId = appUserId;
    return settings;
  }
}
