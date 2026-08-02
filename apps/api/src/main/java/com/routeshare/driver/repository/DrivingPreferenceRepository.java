package com.routeshare.driver.repository;

import com.routeshare.driver.entity.DrivingPreferenceEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DrivingPreferenceRepository extends JpaRepository<DrivingPreferenceEntity, Long> {

  /**
   * D35's "cost you 3 requests last week": how many riders were turned away from this driver's
   * trips by the verified-only rule in the window.
   *
   * <p>Counted from the denial log rather than inferred, because a rider who never saw the trip
   * never made a request there is any other trace of.
   */
  @Query(
      value =
          """
      SELECT COUNT(*)
        FROM routing.eligibility_denial d
        JOIN routing.route_occurrence o ON o.route_occurrence_id = d.route_occurrence_id
        JOIN routing.route_plan p ON p.route_plan_id = o.route_plan_id
       WHERE p.driver_profile_id = :driverProfileId
         AND d.reason = 'NOT_ELIGIBLE_VERIFIED_ONLY'
         AND d.denied_at >= :since
      """,
      nativeQuery = true)
  long countVerifiedOnlyDenials(
      @Param("driverProfileId") long driverProfileId, @Param("since") Instant since);

  /**
   * The share of riders on this driver's own corridors who are verified — the number D35 shows next
   * to the toggle so "verified riders only" is a choice with a size attached to it.
   */
  @Query(
      value =
          """
      SELECT COALESCE(ROUND(
               100.0 * COUNT(*) FILTER (WHERE pp.verification_level = 'VERIFIED')
               / NULLIF(COUNT(*), 0)), 0)
        FROM booking.booking b
        JOIN routing.route_plan p ON p.route_plan_id = b.route_plan_id
        LEFT JOIN passenger.passenger_profile pp ON pp.app_user_id = b.passenger_app_user_id
       WHERE p.driver_profile_id = :driverProfileId
         AND b.created_at >= :since
      """,
      nativeQuery = true)
  int verifiedRiderSharePercent(
      @Param("driverProfileId") long driverProfileId, @Param("since") Instant since);
}
