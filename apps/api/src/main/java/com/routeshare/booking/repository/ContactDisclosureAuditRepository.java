package com.routeshare.booking.repository;

import com.routeshare.booking.entity.ContactDisclosureAuditEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactDisclosureAuditRepository
    extends JpaRepository<ContactDisclosureAuditEntity, Long> {

  /**
   * How many numbers this account has read recently.
   *
   * <p>Number harvesting is the abuse direct dial invites: nothing about a single disclosure looks
   * wrong, and the pattern is the only signal there is.
   */
  @Query(
      value =
          """
      SELECT count(DISTINCT subject_app_user_id)
        FROM booking.contact_disclosure_audit
       WHERE reader_app_user_id = :appUserId
         AND read_at >= :since
      """,
      nativeQuery = true)
  int countDistinctSubjectsSince(@Param("appUserId") long appUserId, @Param("since") Instant since);
}
