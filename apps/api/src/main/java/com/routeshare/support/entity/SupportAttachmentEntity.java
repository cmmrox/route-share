package com.routeshare.support.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "support_attachment", schema = "support")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupportAttachmentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "support_attachment_id")
  private Long id;

  @Column(name = "support_ticket_id", nullable = false)
  private Long ticketId;

  @Column(name = "support_message_id")
  private Long messageId;

  @Column(name = "object_key", nullable = false)
  private String objectKey;

  @Column(nullable = false)
  private String filename;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(name = "uploaded_by_app_user_id", nullable = false)
  private Long uploadedByAppUserId;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public static SupportAttachmentEntity reserve(
      long ticketId,
      String objectKey,
      String filename,
      String contentType,
      long sizeBytes,
      long uploadedByAppUserId) {
    var entity = new SupportAttachmentEntity();
    entity.ticketId = ticketId;
    entity.objectKey = objectKey;
    entity.filename = filename;
    entity.contentType = contentType;
    entity.sizeBytes = sizeBytes;
    entity.uploadedByAppUserId = uploadedByAppUserId;
    return entity;
  }

  public void submit(Instant now) {
    this.submittedAt = now;
  }
}
