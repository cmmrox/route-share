package com.routeshare.rewards.repository;

import com.routeshare.rewards.entity.RewardsLedgerEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardsRepository extends JpaRepository<RewardsLedgerEntity, Long> {
  @Modifying
  @Query(
      value =
          """
          INSERT INTO rewards.referral_code(app_user_id, code)
          VALUES (:appUserId, :code)
          ON CONFLICT (app_user_id) DO NOTHING
          """,
      nativeQuery = true)
  int ensureCode(@Param("appUserId") long appUserId, @Param("code") String code);

  @Query(
      value = "SELECT code FROM rewards.referral_code WHERE app_user_id = :appUserId",
      nativeQuery = true)
  Optional<String> findCode(@Param("appUserId") long appUserId);

  @Query(
      value =
          """
          SELECT rc.app_user_id AS "appUserId", u.phone AS "phone"
            FROM rewards.referral_code rc
            JOIN identity.app_user u ON u.app_user_id = rc.app_user_id
           WHERE rc.code = :code
          """,
      nativeQuery = true)
  Optional<CodeOwnerRow> findCodeOwner(@Param("code") String code);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO rewards.referral_device(app_user_id, device_hash)
          VALUES (:appUserId, :deviceHash)
          ON CONFLICT DO NOTHING
          """,
      nativeQuery = true)
  int rememberDevice(@Param("appUserId") long appUserId, @Param("deviceHash") String deviceHash);

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1 FROM rewards.referral_device
             WHERE app_user_id = :appUserId AND device_hash = :deviceHash
          )
          """,
      nativeQuery = true)
  boolean deviceBelongsTo(
      @Param("appUserId") long appUserId, @Param("deviceHash") String deviceHash);

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1 FROM rewards.referral_edge WHERE referee_app_user_id = :appUserId
          )
          """,
      nativeQuery = true)
  boolean alreadyAttributed(@Param("appUserId") long appUserId);

  @Query(
      value =
          """
          SELECT created_at FROM identity.app_user WHERE app_user_id = :appUserId
          """,
      nativeQuery = true)
  Optional<Instant> accountCreatedAt(@Param("appUserId") long appUserId);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM booking.booking WHERE passenger_app_user_id = :appUserId)",
      nativeQuery = true)
  boolean hasBooking(@Param("appUserId") long appUserId);

  @Query(
      value =
          """
          INSERT INTO rewards.referral_edge
              (referrer_app_user_id, referee_app_user_id, code, attributed_at,
               window_expires_at, max_trips)
          VALUES (:referrerId, :refereeId, :code, :now,
                  :expiresAt, :maxTrips)
          RETURNING referral_edge_id
          """,
      nativeQuery = true)
  long createEdge(
      @Param("referrerId") long referrerId,
      @Param("refereeId") long refereeId,
      @Param("code") String code,
      @Param("now") Instant now,
      @Param("expiresAt") Instant expiresAt,
      @Param("maxTrips") int maxTrips);

  @Query(
      value =
          """
          SELECT rc.app_user_id FROM rewards.referral_code rc
           WHERE rc.app_user_id = :appUserId FOR UPDATE
          """,
      nativeQuery = true)
  Optional<Long> lockAccount(@Param("appUserId") long appUserId);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO rewards.rewards_ledger
              (app_user_id, kind, amount, label, sublabel, source_booking_id,
               source_penalty_id, referral_edge_id, withdrawal_id, occurred_at, idempotency_key)
          VALUES (:appUserId, :kind, :amount, :label, :sublabel, :bookingId,
                  :penaltyId, :edgeId, :withdrawalId, :occurredAt, :idempotencyKey)
          ON CONFLICT (idempotency_key) DO NOTHING
          """,
      nativeQuery = true)
  int insertLedger(
      @Param("appUserId") long appUserId,
      @Param("kind") String kind,
      @Param("amount") BigDecimal amount,
      @Param("label") String label,
      @Param("sublabel") String sublabel,
      @Param("bookingId") Long bookingId,
      @Param("penaltyId") Long penaltyId,
      @Param("edgeId") Long edgeId,
      @Param("withdrawalId") Long withdrawalId,
      @Param("occurredAt") Instant occurredAt,
      @Param("idempotencyKey") String idempotencyKey);

  @Query(
      value =
          """
          SELECT COALESCE(SUM(amount), 0) FROM rewards.rewards_ledger
           WHERE app_user_id = :appUserId
          """,
      nativeQuery = true)
  BigDecimal balance(@Param("appUserId") long appUserId);

  @Query(
      value =
          """
          SELECT rewards_ledger_id AS "id", occurred_at AS "occurredAt", kind AS "kind",
                 label AS "label", sublabel AS "sublabel", amount AS "amount"
            FROM rewards.rewards_ledger
           WHERE app_user_id = :appUserId
           ORDER BY occurred_at DESC, rewards_ledger_id DESC
           LIMIT 100
          """,
      nativeQuery = true)
  List<LedgerRow> ledger(@Param("appUserId") long appUserId);

  @Query(
      value =
          """
          SELECT COALESCE(SUM(amount), 0) FROM rewards.rewards_ledger
           WHERE app_user_id = :appUserId AND kind = 'REFERRAL'
          """,
      nativeQuery = true)
  BigDecimal referralEarned(@Param("appUserId") long appUserId);

  @Query(
      value =
          """
          SELECT COUNT(*) FROM rewards.referral_edge
           WHERE referrer_app_user_id = :appUserId AND status = 'ACTIVE'
          """,
      nativeQuery = true)
  int stillEarning(@Param("appUserId") long appUserId);

  @Query(
      value =
          """
          SELECT e.referral_edge_id AS "edgeId",
                 CASE
                   WHEN COALESCE(pp.full_name, u.display_name, '') ~ '[0-9@]'
                     THEN 'ComiGo member'
                   ELSE concat(
                     split_part(COALESCE(pp.full_name, u.display_name, 'ComiGo member'), ' ', 1),
                     CASE WHEN split_part(COALESCE(pp.full_name, u.display_name, ''), ' ', 2) = ''
                          THEN '' ELSE ' ' || left(split_part(
                            COALESCE(pp.full_name, u.display_name, ''), ' ', 2), 1) || '.' END)
                 END AS "who",
                 CASE WHEN dp.driver_profile_id IS NULL THEN 'RIDER' ELSE 'DRIVER' END AS "role",
                 e.attributed_at AS "joinedAt", e.trips_counted AS "trips",
                 GREATEST(e.max_trips - e.trips_counted, 0) AS "tripsLeft",
                 COALESCE(SUM(l.amount) FILTER (WHERE l.kind = 'REFERRAL'), 0) AS "earned",
                 e.status AS "status"
            FROM rewards.referral_edge e
            JOIN identity.app_user u ON u.app_user_id = e.referee_app_user_id
            LEFT JOIN passenger.passenger_profile pp ON pp.app_user_id = u.app_user_id
            LEFT JOIN driver.driver_profile dp ON dp.app_user_id = u.app_user_id
            LEFT JOIN rewards.rewards_ledger l ON l.referral_edge_id = e.referral_edge_id
           WHERE e.referrer_app_user_id = :appUserId
           GROUP BY e.referral_edge_id, pp.full_name, u.display_name, dp.driver_profile_id
           ORDER BY e.attributed_at DESC
          """,
      nativeQuery = true)
  List<ReferralRow> referrals(@Param("appUserId") long appUserId);

  @Query(
      value =
          """
          SELECT b.booking_id AS "bookingId", b.passenger_app_user_id AS "appUserId",
                 'RIDER' AS "role", q.passenger_pays AS "baseAmount",
                 q.commission_amount AS "commission"
            FROM trip.trip t
            JOIN booking.booking b
              ON b.route_plan_id = t.route_plan_id
             AND (b.route_occurrence_id = t.route_occurrence_id OR t.route_occurrence_id IS NULL)
            JOIN pricing.fare_quote q ON q.booking_id = b.booking_id
           WHERE t.trip_id = :tripId AND b.status IN ('CONFIRMED', 'COMPLETED')
          UNION ALL
          SELECT MIN(b.booking_id) AS "bookingId", dp.app_user_id AS "appUserId",
                 'DRIVER' AS "role", SUM(q.driver_net) AS "baseAmount",
                 SUM(q.commission_amount) AS "commission"
            FROM trip.trip t
            JOIN routing.route_plan rp ON rp.route_plan_id = t.route_plan_id
            JOIN driver.driver_profile dp ON dp.driver_profile_id = rp.driver_profile_id
            JOIN booking.booking b
              ON b.route_plan_id = t.route_plan_id
             AND (b.route_occurrence_id = t.route_occurrence_id OR t.route_occurrence_id IS NULL)
            JOIN pricing.fare_quote q ON q.booking_id = b.booking_id
           WHERE t.trip_id = :tripId AND b.status IN ('CONFIRMED', 'COMPLETED')
           GROUP BY dp.app_user_id
          """,
      nativeQuery = true)
  List<TripParticipantRow> tripParticipants(@Param("tripId") long tripId);

  @Query(
      value =
          """
          SELECT referral_edge_id AS "edgeId", referrer_app_user_id AS "referrerId",
                 referee_app_user_id AS "refereeId", window_expires_at AS "expiresAt",
                 max_trips AS "maxTrips", trips_counted AS "tripsCounted", status AS "status"
            FROM rewards.referral_edge
           WHERE referee_app_user_id = :refereeId
           FOR UPDATE
          """,
      nativeQuery = true)
  Optional<ActiveEdgeRow> lockEdgeForReferee(@Param("refereeId") long refereeId);

  @Modifying
  @Query(
      value =
          """
          UPDATE rewards.referral_edge
             SET trips_counted = trips_counted + 1,
                 status = CASE WHEN trips_counted + 1 >= max_trips
                               THEN 'EXPIRED_TRIPS' ELSE status END
           WHERE referral_edge_id = :edgeId AND status = 'ACTIVE'
          """,
      nativeQuery = true)
  int countTrip(@Param("edgeId") long edgeId);

  @Modifying
  @Query(
      value =
          """
          UPDATE rewards.referral_edge SET status = 'EXPIRED_WINDOW'
           WHERE status = 'ACTIVE' AND window_expires_at <= :now
          """,
      nativeQuery = true)
  int expireWindows(@Param("now") Instant now);

  @Modifying
  @Query(
      value =
          """
          UPDATE passenger.passenger_profile
             SET rewards_auto_apply = :enabled, updated_at = now()
           WHERE app_user_id = :appUserId
          """,
      nativeQuery = true)
  int setAutoApply(@Param("appUserId") long appUserId, @Param("enabled") boolean enabled);

  @Query(
      value =
          """
          SELECT COALESCE(rewards_auto_apply, true)
            FROM passenger.passenger_profile WHERE app_user_id = :appUserId
          """,
      nativeQuery = true)
  Optional<Boolean> autoApply(@Param("appUserId") long appUserId);

  @Query(
      value =
          """
          SELECT amount FROM rewards.rewards_ledger
           WHERE app_user_id = :appUserId AND source_booking_id = :bookingId
             AND kind = 'SPEND' AND idempotency_key = 'booking-credit:' || :bookingId
          """,
      nativeQuery = true)
  Optional<BigDecimal> bookingSpend(
      @Param("appUserId") long appUserId, @Param("bookingId") long bookingId);

  @Query(
      value =
          """
          INSERT INTO rewards.withdrawal(app_user_id, amount)
          VALUES (:appUserId, :amount)
          RETURNING withdrawal_id
          """,
      nativeQuery = true)
  long createWithdrawal(@Param("appUserId") long appUserId, @Param("amount") BigDecimal amount);

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1 FROM rewards.withdrawal
             WHERE app_user_id = :appUserId AND status IN ('QUEUED', 'BATCHED')
          )
          """,
      nativeQuery = true)
  boolean hasOpenWithdrawal(@Param("appUserId") long appUserId);

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1
              FROM driver.driver_profile d
              JOIN driver.driver_payout_profile p
                ON p.driver_profile_id = d.driver_profile_id
             WHERE d.app_user_id = :appUserId
               AND d.verification_status = 'APPROVED'
               AND p.method = 'BANK_TRANSFER'
               AND p.status = 'VERIFIED'
          )
          """,
      nativeQuery = true)
  boolean bankWithdrawalReady(@Param("appUserId") long appUserId);

  @Query(
      value =
          """
          SELECT withdrawal_id AS "id", amount AS "amount", status AS "status",
                 requested_at AS "requestedAt", batched_at AS "batchedAt",
                 paid_at AS "paidAt", failure_reason AS "failureReason"
            FROM rewards.withdrawal WHERE app_user_id = :appUserId
           ORDER BY requested_at DESC, withdrawal_id DESC
          """,
      nativeQuery = true)
  List<WithdrawalRow> withdrawals(@Param("appUserId") long appUserId);

  @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM rewards.rewards_ledger", nativeQuery = true)
  BigDecimal totalBalance();

  interface CodeOwnerRow {
    long getAppUserId();

    String getPhone();
  }

  interface LedgerRow {
    long getId();

    Instant getOccurredAt();

    String getKind();

    String getLabel();

    String getSublabel();

    BigDecimal getAmount();
  }

  interface ReferralRow {
    long getEdgeId();

    String getWho();

    String getRole();

    Instant getJoinedAt();

    int getTrips();

    int getTripsLeft();

    BigDecimal getEarned();

    String getStatus();
  }

  interface TripParticipantRow {
    long getBookingId();

    long getAppUserId();

    String getRole();

    BigDecimal getBaseAmount();

    BigDecimal getCommission();
  }

  interface ActiveEdgeRow {
    long getEdgeId();

    long getReferrerId();

    long getRefereeId();

    Instant getExpiresAt();

    int getMaxTrips();

    int getTripsCounted();

    String getStatus();
  }

  interface WithdrawalRow {
    long getId();

    BigDecimal getAmount();

    String getStatus();

    Instant getRequestedAt();

    Instant getBatchedAt();

    Instant getPaidAt();

    String getFailureReason();
  }
}
