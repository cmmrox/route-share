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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * The last seat, taken twice at once.
 *
 * <p>A counter decrement can be made safe, but it decides the race by arithmetic — both riders
 * asked for "a seat" and one is told there were none. Naming the seats makes it a constraint
 * instead: whoever inserts first holds slot 3, and the loser is refused by the index rather than by
 * whichever transaction happened to read the counter first. That property only exists in a real
 * database under real contention, which is what this proves.
 */
@Testcontainers(disabledWithoutDocker = true)
class SeatInventoryConcurrencyIT {
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
  @DisplayName("07-2: twenty riders take the same seat at once; one holds it and nineteen are told")
  void onlyOneHoldSurvivesForOneSeat() throws Exception {
    long occurrenceId = seedOccurrence("seat-race");
    long seatId = seatSlot(occurrenceId, 1);

    int threads = 20;
    CyclicBarrier allAtOnce = new CyclicBarrier(threads);
    AtomicInteger held = new AtomicInteger();
    AtomicInteger refused = new AtomicInteger();

    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      Callable<Void> take =
          () -> {
            long bookingId = seedBooking(occurrenceId);
            allAtOnce.await(30, TimeUnit.SECONDS);
            try {
              holdSeat(bookingId, seatId);
              held.incrementAndGet();
            } catch (SQLException taken) {
              // 23505 unique_violation — the partial index refused it, not a prior read.
              assertThat(taken.getSQLState()).isEqualTo("23505");
              refused.incrementAndGet();
            }
            return null;
          };
      for (Future<Void> future : pool.invokeAll(Collections.nCopies(threads, take))) {
        future.get(60, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
    }

    assertThat(held.get()).isEqualTo(1);
    assertThat(refused.get()).isEqualTo(threads - 1);
    assertThat(liveHolds(seatId)).isEqualTo(1);
  }

  @Test
  @DisplayName("A released hold puts the seat back on sale; the history of who held it stays")
  void releasedHoldFreesTheSeatWithoutLosingTheTrail() throws Exception {
    long occurrenceId = seedOccurrence("seat-release");
    long seatId = seatSlot(occurrenceId, 1);

    long first = seedBooking(occurrenceId);
    holdSeat(first, seatId);
    execute("UPDATE booking.booking_seat SET released_at = now() WHERE booking_id = " + first);

    long second = seedBooking(occurrenceId);
    holdSeat(second, seatId);

    assertThat(liveHolds(seatId)).isEqualTo(1);
    assertThat(allHolds(seatId)).isEqualTo(2);
  }

  @Test
  @DisplayName("07-1: a CAR occurrence gets one front seat and two in the rear row")
  void backfillNamesTheSlots() throws Exception {
    long occurrenceId = seedOccurrence("seat-map");
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT slot_index, label, sub_label FROM routing.route_occurrence_seat"
                    + " WHERE route_occurrence_id = ? ORDER BY slot_index")) {
      ps.setLong(1, occurrenceId);
      try (ResultSet rs = ps.executeQuery()) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("label")).isEqualTo("Front seat");
        assertThat(rs.getString("sub_label")).isEqualTo("Beside the driver");
        int rest = 0;
        while (rs.next()) {
          assertThat(rs.getString("label")).isEqualTo("Back seat");
          rest++;
        }
        assertThat(rest).isEqualTo(2);
      }
    }
  }

  // ── fixtures ─────────────────────────────────────────────────────────────────────────────────

  private long seedOccurrence(String key) throws SQLException {
    try (Connection c = dataSource.getConnection()) {
      long driverAppUserId =
          id(
              c,
              "INSERT INTO identity.app_user(keycloak_subject, display_name) VALUES ('drv-"
                  + key
                  + "', 'Priya Jayawardena') RETURNING app_user_id");
      long driverProfileId =
          id(
              c,
              "INSERT INTO driver.driver_profile(app_user_id, display_name, verification_status)"
                  + " VALUES ("
                  + driverAppUserId
                  + ", 'Priya', 'APPROVED') RETURNING driver_profile_id");
      long vehicleId =
          id(
              c,
              "INSERT INTO vehicle.vehicle(driver_profile_id, make, model, manufacture_year,"
                  + " color, registration_number, seat_count, status, class_key) VALUES ("
                  + driverProfileId
                  + ", 'Honda', 'Fit', 2019, 'Pearl', 'REG-"
                  + key
                  + "', 3, 'APPROVED', 'CAR') RETURNING vehicle_id");
      long routePlanId =
          id(
              c,
              "INSERT INTO routing.route_plan(driver_profile_id, vehicle_id, origin_label,"
                  + " destination_label, route_line, route_length_m, departure_time, available_seats)"
                  + " VALUES ("
                  + driverProfileId
                  + ", "
                  + vehicleId
                  + ", 'Narahenpita', 'Thunmulla',"
                  + " ST_SetSRID(ST_MakeLine(ST_MakePoint(79.86, 6.93), ST_MakePoint(79.88, 6.90)),"
                  + " 4326), 4500.00, TIMESTAMPTZ '2026-08-02 09:00:00+00', 3) RETURNING route_plan_id");
      long occurrenceId =
          id(
              c,
              "INSERT INTO routing.route_occurrence(route_plan_id, scheduled_departure_at,"
                  + " available_seats) VALUES ("
                  + routePlanId
                  + ", now() + interval '6 hours', 3) RETURNING route_occurrence_id");
      // The application generates slots on publication; seeded rows get them the same way the
      // migration's backfill does.
      execute(
          c,
          "INSERT INTO routing.route_occurrence_seat(route_occurrence_id, slot_index, label,"
              + " sub_label) SELECT "
              + occurrenceId
              + ", s,"
              + " CASE WHEN s = 1 THEN 'Front seat' ELSE 'Back seat' END,"
              + " CASE WHEN s = 1 THEN 'Beside the driver' ELSE 'Rear row' END"
              + " FROM generate_series(1, 3) AS s");
      return occurrenceId;
    }
  }

  private long seedBooking(long occurrenceId) throws SQLException {
    try (Connection c = dataSource.getConnection()) {
      long passengerAppUserId =
          id(
              c,
              "INSERT INTO identity.app_user(keycloak_subject, display_name)"
                  + " VALUES ('pax-' || gen_random_uuid(), 'Dinuka Silva') RETURNING app_user_id");
      return id(
          c,
          "INSERT INTO booking.booking(route_plan_id, route_occurrence_id, passenger_app_user_id,"
              + " seats, status, fare_estimate, pickup, dropoff)"
              + " SELECT o.route_plan_id, o.route_occurrence_id, "
              + passengerAppUserId
              + ", 1, 'CONFIRMED', 197.00,"
              + " ST_SetSRID(ST_MakePoint(79.86, 6.93), 4326),"
              + " ST_SetSRID(ST_MakePoint(79.88, 6.90), 4326)"
              + " FROM routing.route_occurrence o WHERE o.route_occurrence_id = "
              + occurrenceId
              + " RETURNING booking_id");
    }
  }

  private long seatSlot(long occurrenceId, int slotIndex) throws SQLException {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT route_occurrence_seat_id FROM routing.route_occurrence_seat"
                    + " WHERE route_occurrence_id = ? AND slot_index = ?")) {
      ps.setLong(1, occurrenceId);
      ps.setInt(2, slotIndex);
      try (ResultSet rs = ps.executeQuery()) {
        assertThat(rs.next()).isTrue();
        return rs.getLong(1);
      }
    }
  }

  private void holdSeat(long bookingId, long seatId) throws SQLException {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO booking.booking_seat(booking_id, route_occurrence_seat_id)"
                    + " VALUES (?, ?)")) {
      ps.setLong(1, bookingId);
      ps.setLong(2, seatId);
      ps.executeUpdate();
    }
  }

  private int liveHolds(long seatId) throws SQLException {
    return count(
        "SELECT count(*) FROM booking.booking_seat WHERE route_occurrence_seat_id = "
            + seatId
            + " AND released_at IS NULL");
  }

  private int allHolds(long seatId) throws SQLException {
    return count(
        "SELECT count(*) FROM booking.booking_seat WHERE route_occurrence_seat_id = " + seatId);
  }

  private int count(String sql) throws SQLException {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      assertThat(rs.next()).isTrue();
      return rs.getInt(1);
    }
  }

  private long id(Connection c, String sql) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      assertThat(rs.next()).isTrue();
      return rs.getLong(1);
    }
  }

  private void execute(String sql) throws SQLException {
    try (Connection c = dataSource.getConnection()) {
      execute(c, sql);
    }
  }

  private void execute(Connection c, String sql) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.executeUpdate();
    }
  }
}
