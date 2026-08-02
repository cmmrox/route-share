package com.routeshare.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * One trip per occurrence, proven where it is actually enforced.
 *
 * <p>Two passengers taking the last two seats are two transactions racing on one occurrence. The
 * service reads-after-insert and would happily create a second trip if the database allowed it, so
 * what makes this safe is {@code trip_route_occurrence_uk} — and an index is only proven by a real
 * database under real contention. A second trip on one occurrence would give the same departure two
 * start windows, two sweeps and two auto-cancels.
 */
@Testcontainers(disabledWithoutDocker = true)
class TripMaterialisationIT {
  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(
              DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("routeshare")
          .withUsername("routeshare")
          .withPassword("routeshare_dev_password");

  private static DataSource dataSource;

  @BeforeAll
  static void migrate() {
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
  }

  @Test
  void manySimultaneousFirstBookingsOnOneOccurrenceProduceExactlyOneTrip() throws Exception {
    long occurrenceId = seedOccurrence("concurrent-materialisation");

    int threads = 20;
    CyclicBarrier allAtOnce = new CyclicBarrier(threads);
    AtomicInteger inserted = new AtomicInteger();

    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      Callable<Void> materialise =
          () -> {
            allAtOnce.await(30, TimeUnit.SECONDS);
            try (Connection c = dataSource.getConnection();
                PreparedStatement ps =
                    c.prepareStatement(
                        """
                        INSERT INTO trip.trip(route_plan_id, route_occurrence_id, status)
                        SELECT ro.route_plan_id, ro.route_occurrence_id, 'SCHEDULED'
                        FROM routing.route_occurrence ro
                        WHERE ro.route_occurrence_id = ?
                        ON CONFLICT (route_occurrence_id)
                          WHERE route_occurrence_id IS NOT NULL DO NOTHING
                        """)) {
              ps.setLong(1, occurrenceId);
              inserted.addAndGet(ps.executeUpdate());
            }
            return null;
          };

      for (Future<Void> f : pool.invokeAll(Collections.nCopies(threads, materialise))) {
        f.get(60, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
    }

    // Exactly one insert takes effect; the other nineteen are absorbed by the conflict clause
    // rather than raising, because a passenger's booking must not fail for losing this race.
    assertThat(inserted.get()).isEqualTo(1);
    assertThat(countTripsForOccurrence(occurrenceId)).isEqualTo(1);
  }

  /**
   * The index is partial, and it has to be: trips predating the occurrence model carry a NULL
   * occurrence, and collapsing those onto one key would make the second such trip impossible.
   */
  @Test
  void tripsWithNoOccurrenceBehindThemAreNotConstrainedAgainstEachOther() throws Exception {
    long occurrenceId = seedOccurrence("null-occurrence-trips");
    long routePlanId = routePlanOf(occurrenceId);

    insertTripWithoutOccurrence(routePlanId);
    insertTripWithoutOccurrence(routePlanId);

    assertThat(countTripsWithoutOccurrence(routePlanId)).isEqualTo(2);
  }

  private long seedOccurrence(String key) throws SQLException {
    try (Connection c = dataSource.getConnection()) {
      long appUserId =
          returningId(
              c,
              "INSERT INTO identity.app_user(keycloak_subject, display_name) VALUES (?, 'Driver')"
                  + " RETURNING app_user_id",
              key);
      long driverProfileId =
          returningId(
              c,
              "INSERT INTO driver.driver_profile(app_user_id, display_name, verification_status)"
                  + " VALUES ("
                  + appUserId
                  + ", ?, 'APPROVED') RETURNING driver_profile_id",
              "Driver " + key);
      long vehicleId =
          returningId(
              c,
              "INSERT INTO vehicle.vehicle(driver_profile_id, make, model, manufacture_year, color,"
                  + " registration_number, seat_count, status, class_key) VALUES ("
                  + driverProfileId
                  + ", 'Toyota', 'Aqua', 2018, 'Silver', ?, 3, 'APPROVED', 'CAR')"
                  + " RETURNING vehicle_id",
              "REG-" + key);
      long routePlanId =
          returningId(
              c,
              "INSERT INTO routing.route_plan(driver_profile_id, vehicle_id, origin_label,"
                  + " destination_label, route_line, route_length_m, departure_time,"
                  + " available_seats) VALUES ("
                  + driverProfileId
                  + ", "
                  + vehicleId
                  + ", ?, 'Kandy', ST_SetSRID(ST_MakeLine(ST_MakePoint(79.86, 6.93),"
                  + " ST_MakePoint(80.63, 7.29)), 4326), 115000.00,"
                  + " TIMESTAMPTZ '2026-08-02 09:00:00+00', 3) RETURNING route_plan_id",
              "Colombo " + key);
      return returningId(
          c,
          "INSERT INTO routing.route_occurrence(route_plan_id, scheduled_departure_at,"
              + " available_seats) VALUES ("
              + routePlanId
              + ", TIMESTAMPTZ '2026-08-02 09:00:00+00', 3) RETURNING route_occurrence_id",
          null);
    }
  }

  private long returningId(Connection c, String sql, String parameter) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      if (parameter != null) {
        ps.setString(1, parameter);
      }
      try (ResultSet rs = ps.executeQuery()) {
        assertThat(rs.next()).isTrue();
        return rs.getLong(1);
      }
    }
  }

  private long routePlanOf(long occurrenceId) throws SQLException {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT route_plan_id FROM routing.route_occurrence WHERE route_occurrence_id = ?")) {
      ps.setLong(1, occurrenceId);
      try (ResultSet rs = ps.executeQuery()) {
        assertThat(rs.next()).isTrue();
        return rs.getLong(1);
      }
    }
  }

  private void insertTripWithoutOccurrence(long routePlanId) throws SQLException {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO trip.trip(route_plan_id, status) VALUES (?, 'SCHEDULED')")) {
      ps.setLong(1, routePlanId);
      ps.executeUpdate();
    }
  }

  private int countTripsForOccurrence(long occurrenceId) throws SQLException {
    return count("SELECT count(*) FROM trip.trip WHERE route_occurrence_id = ?", occurrenceId);
  }

  private int countTripsWithoutOccurrence(long routePlanId) throws SQLException {
    return count(
        "SELECT count(*) FROM trip.trip WHERE route_plan_id = ? AND route_occurrence_id IS NULL",
        routePlanId);
  }

  private int count(String sql, long parameter) throws SQLException {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setLong(1, parameter);
      try (ResultSet rs = ps.executeQuery()) {
        assertThat(rs.next()).isTrue();
        return rs.getInt(1);
      }
    }
  }
}
