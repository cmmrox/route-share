package com.routeshare.routing.repository;

import com.routeshare.routing.entity.RouteScheduleRuleEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouteScheduleRuleRepository extends JpaRepository<RouteScheduleRuleEntity, Long> {
  @Query(
      value =
          """
      INSERT INTO routing.route_schedule_rule(route_plan_id, schedule_type, start_at, end_at)
      VALUES (:routePlanId, 'ONE_TIME', :startAt, :startAt)
      RETURNING route_schedule_rule_id
      """,
      nativeQuery = true)
  long insertOneTimeRule(@Param("routePlanId") long routePlanId, @Param("startAt") Instant startAt);

  @Query(
      value =
          """
      INSERT INTO routing.route_schedule_rule(route_plan_id, schedule_type, start_at, end_at, days_of_week)
      VALUES (:routePlanId, 'RECURRING', :startAt, :endAt,
              CASE WHEN :daysCsv = '' THEN '{}'::text[] ELSE string_to_array(:daysCsv, ',') END)
      RETURNING route_schedule_rule_id
      """,
      nativeQuery = true)
  long insertRecurringRule(
      @Param("routePlanId") long routePlanId,
      @Param("startAt") Instant startAt,
      @Param("endAt") Instant endAt,
      @Param("daysCsv") String daysCsv);

  @Query(
      value =
          """
      SELECT sr.route_schedule_rule_id AS "ruleId", sr.route_plan_id AS "routePlanId",
             sr.start_at AS "startAt", sr.end_at AS "endAt",
             array_to_string(sr.days_of_week, ',') AS "daysCsv", sr.status AS "status",
             rp.origin_label AS "originLabel", rp.destination_label AS "destinationLabel"
      FROM routing.route_schedule_rule sr
      JOIN routing.route_plan rp ON rp.route_plan_id = sr.route_plan_id
      JOIN driver.driver_profile d ON d.driver_profile_id = rp.driver_profile_id
      WHERE d.app_user_id = :appUserId AND sr.schedule_type = 'RECURRING'
      ORDER BY sr.route_schedule_rule_id DESC
      """,
      nativeQuery = true)
  List<RecurringRuleRow> findRecurringRulesForDriver(@Param("appUserId") long appUserId);

  @Query(
      value =
          """
      SELECT sr.route_schedule_rule_id AS "ruleId", sr.route_plan_id AS "routePlanId",
             sr.start_at AS "startAt", sr.end_at AS "endAt",
             array_to_string(sr.days_of_week, ',') AS "daysCsv", sr.status AS "status",
             rp.origin_label AS "originLabel", rp.destination_label AS "destinationLabel"
      FROM routing.route_schedule_rule sr
      JOIN routing.route_plan rp ON rp.route_plan_id = sr.route_plan_id
      JOIN driver.driver_profile d ON d.driver_profile_id = rp.driver_profile_id
      WHERE sr.route_schedule_rule_id = :ruleId AND d.app_user_id = :appUserId
        AND sr.schedule_type = 'RECURRING'
      """,
      nativeQuery = true)
  Optional<RecurringRuleRow> findRecurringRuleForDriver(
      @Param("ruleId") long ruleId, @Param("appUserId") long appUserId);

  @Modifying
  @Query(
      value =
          """
      UPDATE routing.route_schedule_rule SET status = :status
      WHERE route_schedule_rule_id = :ruleId
      """,
      nativeQuery = true)
  int updateStatus(@Param("ruleId") long ruleId, @Param("status") String status);

  interface RecurringRuleRow {
    long getRuleId();

    long getRoutePlanId();

    Instant getStartAt();

    Instant getEndAt();

    String getDaysCsv();

    String getStatus();

    String getOriginLabel();

    String getDestinationLabel();
  }
}
