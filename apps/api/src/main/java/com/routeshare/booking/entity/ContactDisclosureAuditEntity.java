package com.routeshare.booking.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One read of one person's phone number.
 *
 * <p>Written on <em>every</em> read, including repeats. "He looked up my number eleven times in an
 * hour" is a pattern only repeated rows can show, and this table is the trail a harassment report
 * is investigated from — deduplicating it would destroy the evidence it exists to keep.
 */
@Entity
@Table(name = "contact_disclosure_audit", schema = "booking")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContactDisclosureAuditEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "contact_disclosure_audit_id")
  private Long id;

  @Column(name = "booking_id", nullable = false)
  private Long bookingId;

  @Column(name = "reader_app_user_id", nullable = false)
  private Long readerAppUserId;

  @Column(name = "subject_app_user_id", nullable = false)
  private Long subjectAppUserId;

  @Column(name = "read_at", insertable = false, updatable = false)
  private Instant readAt;

  public static ContactDisclosureAuditEntity of(
      long bookingId, long readerAppUserId, long subjectAppUserId) {
    var entity = new ContactDisclosureAuditEntity();
    entity.bookingId = bookingId;
    entity.readerAppUserId = readerAppUserId;
    entity.subjectAppUserId = subjectAppUserId;
    return entity;
  }
}
