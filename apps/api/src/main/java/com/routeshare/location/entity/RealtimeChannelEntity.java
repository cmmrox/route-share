package com.routeshare.location.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "realtime_channel", schema = "location")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RealtimeChannelEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "realtime_channel_id")
  private Long id;

  @Column(name = "app_user_id")
  private Long appUserId;

  @Column(name = "connection_id")
  private String connectionId;

  @Column(name = "connected_at")
  private Instant connectedAt;

  @Column(name = "last_seen_at")
  private Instant lastSeenAt;

  private String transport;
}
