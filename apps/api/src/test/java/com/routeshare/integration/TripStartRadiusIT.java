package com.routeshare.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * The predicate that defines slice 09, against real PostGIS.
 *
 * <p>Four drivers start 2, 6, 14 and 25 km from one pickup point, all on corridors that pass it.
 * The old rule — how close a route line comes to the rider — would return all four at any radius,
 * because every one of them drives past her. The new rule is about where each driver's trip
 * <em>begins</em>, and only a real geometry can tell the two apart.
 *
 * <p>Also pins the two properties a unit test cannot reach: that the filtered-out count is exact
 * and comes from the same statement, and that the query seeks the GIST index rather than scanning
 * every published route in the country.
 */
@Testcontainers(disabledWithoutDocker = true)
class TripStartRadiusIT {

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(
              DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("routeshare")
          .withUsername("routeshare")
          .withPassword("routeshare_dev_password");

  /**
   * The rider stands here. Every seeded driver drives past it; they start at different distances.
   */
  private static final double PICKUP_LAT = 6.9271;

  private static final double PICKUP_LNG = 79.8612;

  private static DataSource dataSource;

  @BeforeAll
  static void migrateAndSeed() throws Exception {
    Flyway.configure()
        .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
        .locations("classpath:db/migration")
        .load()
        .migrate();
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(postgres.getJdbcUrl());
    ds.setUser(postgres.getUsername());
    ds.setPassword(postgres.getPassword());
    dataSource = ds;
    seed();
  }

  @Test
  @DisplayName("09-1: at 20 km the drivers starting 2, 6 and 14 km away are returned; 25 km is not")
  void twentyKilometres() throws Exception {
    assertThat(labelsWithin(20_000)).containsExactlyInAnyOrder("D2", "D6", "D14");
  }

  @Test
  @DisplayName("09-2: at 10 km only the drivers starting 2 and 6 km away remain")
  void tenKilometres() throws Exception {
    assertThat(labelsWithin(10_000)).containsExactlyInAnyOrder("D2", "D6");
  }

  @Test
  @DisplayName("09-3: at 5 km only the driver starting 2 km away remains")
  void fiveKilometres() throws Exception {
    assertThat(labelsWithin(5_000)).containsExactly("D2");
  }

  @Test
  @DisplayName("09-6: the filtered-out count at 10 km is exactly 2, from the same statement")
  void filteredOutCountIsExact() throws Exception {
    // Not "roughly right": P04 prints this next to the list, so a rider can subtract it herself.
    assertThat(filteredOutWithin(10_000)).isEqualTo(2);
    assertThat(filteredOutWithin(20_000)).isEqualTo(1);
    assertThat(filteredOutWithin(5_000)).isEqualTo(3);
  }

  @Test
  @DisplayName("the count and the list always add back to the same total")
  void countsReconcile() throws Exception {
    for (int radius : new int[] {5_000, 10_000, 20_000}) {
      assertThat(labelsWithin(radius).size() + filteredOutWithin(radius))
          .as("returned + filtered out must equal every candidate on the corridor at %d m", radius)
          .isEqualTo(4);
    }
  }

  @Test
  @DisplayName("09-7: startsKmAway is projected, not just tested")
  void startsKmAwayIsProjected() throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                """
                SELECT p.origin_label,
                       ROUND((ST_Distance(p.origin_point::geography,
                              ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography) / 1000)::numeric, 0)
                  FROM routing.route_plan p
                 ORDER BY p.origin_label
                """)) {
      ps.setDouble(1, PICKUP_LNG);
      ps.setDouble(2, PICKUP_LAT);
      var seen = new java.util.LinkedHashMap<String, Integer>();
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          seen.put(rs.getString(1), rs.getInt(2));
        }
      }
      // The card shows this number; the filter used the same one. Computing it twice is how they
      // come to disagree.
      assertThat(seen).containsEntry("D2", 2).containsEntry("D6", 6).containsEntry("D14", 14);
    }
  }

  @Test
  @DisplayName("09-19: the radius seeks the GIST index rather than scanning every route")
  void queryUsesTheGistIndex() throws Exception {
    try (Connection c = dataSource.getConnection();
        Statement s = c.createStatement()) {
      // Volume is the point. On four rows the planner reads the whole table and is right to, so an
      // EXPLAIN over the fixture would prove nothing about the query that matters — the one running
      // against every published route in the country. These rows sit far outside the search radius
      // and are removed again, so no other assertion here sees them.
      s.execute(
          """
          INSERT INTO routing.route_plan(driver_profile_id, vehicle_id, origin_label,
                                         destination_label, route_line, departure_time,
                                         available_seats, status, route_length_m)
          SELECT d.driver_profile_id, v.vehicle_id, 'BULK', 'Bulk',
                 ST_SetSRID(ST_MakeLine(ARRAY[
                   ST_MakePoint(80.0 + (g % 100) * 0.01, 8.0 + (g / 100) * 0.01),
                   ST_MakePoint(80.1 + (g % 100) * 0.01, 8.1 + (g / 100) * 0.01)]), 4326),
                 now() + interval '5 hours', 3, 'PUBLISHED', 9500
            FROM generate_series(1, 5000) AS g,
                 driver.driver_profile d
            JOIN vehicle.vehicle v ON v.driver_profile_id = d.driver_profile_id
          """);
      // Without statistics the planner is still guessing, and a guess is not what this asserts.
      s.execute("ANALYZE routing.route_plan");

      var plan = new StringBuilder();
      try (ResultSet rs =
          s.executeQuery(
              """
              EXPLAIN SELECT route_plan_id FROM routing.route_plan
               WHERE ST_DWithin(origin_point::geography,
                                ST_SetSRID(ST_MakePoint(79.8612, 6.9271), 4326)::geography, 20000)
              """)) {
        while (rs.next()) {
          plan.append(rs.getString(1)).append('\n');
        }
      }
      try {
        // A sequential scan here is a production incident waiting for the route table to grow:
        // this is the hottest query in the product.
        assertThat(plan.toString()).contains("idx_route_plan_origin");
        assertThat(plan.toString()).doesNotContain("Seq Scan");
      } finally {
        s.execute("DELETE FROM routing.route_plan WHERE origin_label = 'BULK'");
        s.execute("ANALYZE routing.route_plan");
      }
    }
  }

  @Test
  @DisplayName(
      "the origin point is kept in step with the route line by the database, not by callers")
  void originPointIsMaintainedByTrigger() throws Exception {
    try (Connection c = dataSource.getConnection();
        Statement s = c.createStatement()) {
      // Inserted without an origin_point at all: a caller that forgets must not be able to file a
      // trip at the wrong place, because nothing downstream could detect it.
      s.execute(
          """
          INSERT INTO routing.route_plan(driver_profile_id, vehicle_id, origin_label,
                                         destination_label, route_line, departure_time,
                                         available_seats, status, route_length_m)
          SELECT d.driver_profile_id, v.vehicle_id, 'TRIGGER', 'Fort',
                 ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(80.10, 7.10),
                                              ST_MakePoint(80.20, 7.20)]), 4326),
                 now() + interval '3 hours', 3, 'PUBLISHED', 9500
            FROM driver.driver_profile d
            JOIN vehicle.vehicle v ON v.driver_profile_id = d.driver_profile_id
           LIMIT 1
          """);
      try (ResultSet rs =
          s.executeQuery(
              """
              SELECT ST_Equals(origin_point, ST_StartPoint(route_line))
                FROM routing.route_plan WHERE origin_label = 'TRIGGER'
              """)) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getBoolean(1)).isTrue();
      }
    }
  }

  @Test
  @DisplayName("09-13: an ineligible trip is excluded inside the query, so the counts stay honest")
  void eligibilityIsAppliedInsideTheQuery() throws Exception {
    try (Connection c = dataSource.getConnection();
        Statement s = c.createStatement()) {
      s.execute(
          """
          UPDATE routing.route_occurrence o SET verified_riders_only = true
            FROM routing.route_plan p
           WHERE p.route_plan_id = o.route_plan_id AND p.origin_label = 'D6'
          """);
    }
    try {
      // An unverified rider does not see it, and — the part a post-filter would get wrong — it is
      // not counted as "filtered out by radius" either. It was never a candidate for her.
      assertThat(labelsWithin(20_000, false)).containsExactlyInAnyOrder("D2", "D14");
      assertThat(filteredOutWithin(20_000, false)).isEqualTo(1);
    } finally {
      try (Connection c = dataSource.getConnection();
          Statement s = c.createStatement()) {
        s.execute("UPDATE routing.route_occurrence SET verified_riders_only = false");
      }
    }
  }

  @Test
  @DisplayName("09-10/09-11: paging is stable because every sort ends on the occurrence id")
  void pagingIsStable() throws Exception {
    // Two departures are deliberately identical, which is exactly when an unstable sort repeats one
    // row on page 1 and drops another from page 2.
    var firstPage = pagedOccurrenceIds(0, 2);
    var secondPage = pagedOccurrenceIds(2, 2);
    var combined = new ArrayList<Long>(firstPage);
    combined.addAll(secondPage);

    assertThat(combined).doesNotHaveDuplicates();
    assertThat(combined).hasSize(4);
    assertThat(pagedOccurrenceIds(0, 2)).isEqualTo(firstPage);
  }

  // ── helpers ────────────────────────────────────────────────────────────────────────────────────

  private List<String> labelsWithin(int radiusMeters) throws Exception {
    return labelsWithin(radiusMeters, true);
  }

  private List<String> labelsWithin(int radiusMeters, boolean riderVerified) throws Exception {
    var labels = new ArrayList<String>();
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement(candidateSql(false))) {
      ps.setDouble(1, PICKUP_LNG);
      ps.setDouble(2, PICKUP_LAT);
      ps.setInt(3, radiusMeters);
      ps.setBoolean(4, riderVerified);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          labels.add(rs.getString(1));
        }
      }
    }
    return labels;
  }

  private int filteredOutWithin(int radiusMeters) throws Exception {
    return filteredOutWithin(radiusMeters, true);
  }

  private int filteredOutWithin(int radiusMeters, boolean riderVerified) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement(candidateSql(true))) {
      ps.setDouble(1, PICKUP_LNG);
      ps.setDouble(2, PICKUP_LAT);
      ps.setInt(3, radiusMeters);
      ps.setBoolean(4, riderVerified);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : 0;
      }
    }
  }

  /** The shape of the real query: one statement, both aggregates, the radius applied last. */
  private static String candidateSql(boolean countOnly) {
    String projection = countOnly ? "COUNT(*) FILTER (WHERE NOT within)" : "origin_label";
    String tail = countOnly ? "" : " WHERE within ORDER BY origin_label";
    return """
        SELECT %s FROM (
          SELECT p.origin_label,
                 ST_Distance(p.origin_point::geography,
                             ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography) <= ? AS within
            FROM routing.route_occurrence o
            JOIN routing.route_plan p ON p.route_plan_id = o.route_plan_id
           WHERE o.status = 'PUBLISHED' AND p.status = 'PUBLISHED'
             AND (o.verified_riders_only = false OR CAST(? AS BOOLEAN))
        ) AS candidates%s
        """
        .formatted(projection, tail);
  }

  private List<Long> pagedOccurrenceIds(int offset, int size) throws Exception {
    var ids = new ArrayList<Long>();
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                """
                SELECT o.route_occurrence_id
                  FROM routing.route_occurrence o
                  JOIN routing.route_plan p ON p.route_plan_id = o.route_plan_id
                 WHERE o.status = 'PUBLISHED'
                 ORDER BY o.scheduled_departure_at ASC, o.route_occurrence_id ASC
                OFFSET ? LIMIT ?
                """)) {
      ps.setInt(1, offset);
      ps.setInt(2, size);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          ids.add(rs.getLong(1));
        }
      }
    }
    return ids;
  }

  private static void seed() throws Exception {
    try (Connection c = dataSource.getConnection();
        Statement s = c.createStatement()) {
      s.execute(
          """
          INSERT INTO identity.app_user(keycloak_subject, display_name)
          VALUES ('driver-sub', 'Nimali')
          """);
      s.execute(
          """
          INSERT INTO driver.driver_profile(app_user_id, display_name, verification_status)
          SELECT app_user_id, 'Nimali', 'APPROVED'
            FROM identity.app_user WHERE keycloak_subject = 'driver-sub'
          """);
      s.execute(
          """
          INSERT INTO vehicle.vehicle(driver_profile_id, make, model, manufacture_year, color,
                                      registration_number, seat_count, status, class_key)
          SELECT driver_profile_id, 'Toyota', 'Aqua', 2018, 'Silver', 'WP-CAB-9001', 3,
                 'APPROVED', 'CAR'
            FROM driver.driver_profile LIMIT 1
          """);

      // Roughly 0.009 degrees of latitude is a kilometre in Colombo. Each route starts that far
      // north of the pickup and runs south through it, so every one of them passes the rider —
      // which is the whole point: only the *start* distance separates them.
      seedRoute(s, "D2", 2);
      seedRoute(s, "D6", 6);
      seedRoute(s, "D14", 14);
      seedRoute(s, "D25", 25);
    }
  }

  private static void seedRoute(Statement s, String label, int kmAway) throws Exception {
    double startLat = PICKUP_LAT + (kmAway * 0.009);
    s.execute(
        """
        INSERT INTO routing.route_plan(driver_profile_id, vehicle_id, origin_label,
                                       destination_label, route_line, departure_time,
                                       available_seats, status, route_length_m)
        SELECT d.driver_profile_id, v.vehicle_id, '%s', 'Fort',
               ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(%f, %f),
                                            ST_MakePoint(%f, %f)]), 4326),
               now() + interval '2 hours', 3, 'PUBLISHED', 9500
          FROM driver.driver_profile d
          JOIN vehicle.vehicle v ON v.driver_profile_id = d.driver_profile_id
         LIMIT 1
        """
            .formatted(label, PICKUP_LNG, startLat, PICKUP_LNG, PICKUP_LAT - 0.02));
    // Two of the four share a departure time on purpose, so the paging test has a real tie to
    // break rather than an accidental total order.
    s.execute(
        """
        INSERT INTO routing.route_occurrence(route_plan_id, scheduled_departure_at,
                                             available_seats, status)
        SELECT route_plan_id, now() + interval '2 hours', 3, 'PUBLISHED'
          FROM routing.route_plan WHERE origin_label = '%s'
        """
            .formatted(label));
  }
}
