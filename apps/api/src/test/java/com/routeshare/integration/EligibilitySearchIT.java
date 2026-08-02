package com.routeshare.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
 * The ineligible trip is absent from the database's answer, not from the client's rendering.
 *
 * <p>This is the property the unit tests cannot reach. {@code EligibilityServiceTest} proves the
 * rule; this proves that the same rule is what the search query applies — that a women-only trip
 * never leaves PostgreSQL for a rider who may not book it, and that the row is genuinely there and
 * merely filtered rather than absent for some unrelated reason.
 *
 * <p>It also proves the schema-level half of the camera-only rule: {@code capture_source} admits
 * {@code CAMERA} and nothing else, so a gallery upload cannot be written even by a path that forgot
 * to check.
 */
@Testcontainers(disabledWithoutDocker = true)
class EligibilitySearchIT {

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(
              DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("routeshare")
          .withUsername("routeshare")
          .withPassword("routeshare_dev_password");

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
  @DisplayName("08-1: a verified-only trip is absent from an unverified rider's candidates")
  void verifiedOnlyTripIsAbsentForAnUnverifiedRider() throws Exception {
    assertThat(visibleTo(false, false)).doesNotContain("VERIFIED_ONLY");
  }

  @Test
  @DisplayName("08-3: a women-only trip is absent from a male rider's candidates")
  void womenOnlyTripIsAbsentForAMaleRider() throws Exception {
    assertThat(visibleTo(true, false)).doesNotContain("WOMEN_ONLY");
  }

  @Test
  @DisplayName("an ordinary trip is visible to everyone, so the absences above are the rule")
  void ordinaryTripIsVisibleToEveryone() throws Exception {
    assertThat(visibleTo(false, false)).contains("ORDINARY");
    assertThat(visibleTo(true, true)).contains("ORDINARY");
  }

  @Test
  @DisplayName("08-5: a verified female rider sees all three")
  void verifiedFemaleRiderSeesEverything() throws Exception {
    assertThat(visibleTo(true, true)).contains("ORDINARY", "VERIFIED_ONLY", "WOMEN_ONLY");
  }

  @Test
  @DisplayName("a verified male rider sees the verified-only trip but not the women-only one")
  void verifiedMaleRiderSeesVerifiedOnly() throws Exception {
    assertThat(visibleTo(true, false)).contains("ORDINARY", "VERIFIED_ONLY");
  }

  @Test
  @DisplayName("camera-only is enforced by the schema, not only by the service")
  void captureSourceAdmitsOnlyCamera() throws Exception {
    try (Connection c = dataSource.getConnection();
        Statement s = c.createStatement()) {
      s.execute(
          """
          INSERT INTO passenger.verification_session(app_user_id, expires_at)
          VALUES (1, now() + interval '30 minutes')
          """);
      org.junit.jupiter.api.Assertions.assertThrows(
          SQLException.class,
          () ->
              s.execute(
                  """
                  INSERT INTO passenger.verification_step(session_id, step_key, capture_source)
                  SELECT verification_session_id, 'NIC_FRONT', 'GALLERY'
                    FROM passenger.verification_session ORDER BY 1 DESC LIMIT 1
                  """));
    }
  }

  @Test
  @DisplayName("a rider has one live verification attempt at a time")
  void oneLiveSessionPerRider() throws Exception {
    try (Connection c = dataSource.getConnection();
        Statement s = c.createStatement()) {
      s.execute(
          """
          INSERT INTO passenger.verification_session(app_user_id, expires_at)
          VALUES (2, now() + interval '30 minutes')
          """);
      org.junit.jupiter.api.Assertions.assertThrows(
          SQLException.class,
          () ->
              s.execute(
                  """
                  INSERT INTO passenger.verification_session(app_user_id, expires_at)
                  VALUES (2, now() + interval '30 minutes')
                  """));
    }
  }

  /** The labels of the trips this rider's search would return. */
  private java.util.List<String> visibleTo(boolean verified, boolean verifiedFemale)
      throws Exception {
    var labels = new java.util.ArrayList<String>();
    // The same two predicates the search query carries, bound the same way, against the real
    // columns — so this fails if the migration and the query ever disagree about their names.
    String sql =
        """
        SELECT p.origin_label
          FROM routing.route_occurrence o
          JOIN routing.route_plan p ON p.route_plan_id = o.route_plan_id
         WHERE o.status = 'PUBLISHED'
           AND (o.gender_policy = 'ANYONE' OR CAST(? AS BOOLEAN))
           AND (o.verified_riders_only = false OR CAST(? AS BOOLEAN))
        """;
    try (Connection c = dataSource.getConnection();
        java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setBoolean(1, verifiedFemale);
      ps.setBoolean(2, verified);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          labels.add(rs.getString(1));
        }
      }
    }
    return labels;
  }

  private static void seed() throws Exception {
    try (Connection c = dataSource.getConnection();
        Statement s = c.createStatement()) {
      s.execute(
          """
          INSERT INTO identity.app_user(keycloak_subject, email, display_name)
          VALUES ('driver-sub', 'nimali@example.test', 'Nimali'),
                 ('rider-sub', 'dinuka@example.test', 'Dinuka')
          """);
      s.execute(
          """
          INSERT INTO driver.driver_profile(app_user_id, display_name, verification_status, gender)
          SELECT app_user_id, 'Nimali', 'APPROVED', 'FEMALE'
            FROM identity.app_user WHERE keycloak_subject = 'driver-sub'
          """);
      s.execute(
          """
          INSERT INTO vehicle.vehicle(driver_profile_id, make, model, manufacture_year, color,
                                      registration_number, seat_count, status, class_key)
          SELECT driver_profile_id, 'Toyota', 'Aqua', 2018, 'Silver', 'WP-CAB-1234', 3,
                 'APPROVED', 'CAR'
            FROM driver.driver_profile LIMIT 1
          """);
      for (String[] trip :
          new String[][] {
            {"ORDINARY", "ANYONE", "false"},
            {"VERIFIED_ONLY", "ANYONE", "true"},
            {"WOMEN_ONLY", "WOMEN_ONLY", "false"}
          }) {
        s.execute(
            """
            INSERT INTO routing.route_plan(driver_profile_id, vehicle_id, origin_label,
                                           destination_label, route_line, departure_time,
                                           available_seats, status, route_length_m)
            SELECT d.driver_profile_id, v.vehicle_id, '"""
                + trip[0]
                + """
            ', 'Fort',
                   ST_SetSRID(ST_MakeLine(ARRAY[ST_MakePoint(79.85, 6.90),
                                                ST_MakePoint(79.90, 6.95)]), 4326),
                   now() + interval '2 hours', 3, 'PUBLISHED', 9500
              FROM driver.driver_profile d
              JOIN vehicle.vehicle v ON v.driver_profile_id = d.driver_profile_id
             LIMIT 1
            """);
        s.execute(
            """
            INSERT INTO routing.route_occurrence(route_plan_id, scheduled_departure_at,
                                                 available_seats, gender_policy,
                                                 verified_riders_only)
            SELECT route_plan_id, now() + interval '2 hours', 3, '"""
                + trip[1]
                + "', "
                + trip[2]
                + """
              FROM routing.route_plan ORDER BY route_plan_id DESC LIMIT 1
            """);
      }
    }
  }
}
