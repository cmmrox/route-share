package com.routeshare.routing.repository;

import com.routeshare.routing.entity.RouteScheduleRuleEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
