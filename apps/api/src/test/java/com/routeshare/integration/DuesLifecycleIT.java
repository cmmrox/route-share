package com.routeshare.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
 * The dues lifecycle, and the three money guarantees {@code V033} makes rather than states.
 *
 * <p>Each of these is a property the unit tests cannot prove. A split that re-adds, a beneficiary
 * total that matches the victim half, and one assessment per trigger under real contention are all
 * guarantees of the schema; against mocks they only demonstrate that the code took the branch it
 * was told to take.
 */
@Testcontainers(disabledWithoutDocker = true)
class DuesLifecycleIT {
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
  @DisplayName("A cash fee is carried to the next booking and settled by the one that charged it")
  void duesAreCarriedThenSettled() throws Exception {
    Fixture f = seed("dues-settled");
    long penaltyId = insertPenalty(f, "PASSENGER_NO_SHOW", "49.00", "25.00", "24.00", "DUES");
    long dueId = insertDue(f, penaltyId, "49.00");

    // P09d: attached to the next checkout, but still outstanding — the line is shown before the
    // money moves, and a booking that never starts must leave the fee owing.
    execute(
        "UPDATE penalty.passenger_due SET settled_booking_id = "
            + f.bookingId
            + " WHERE passenger_due_id = "
            + dueId);
    assertThat(statusOf(dueId)).isEqualTo("OUTSTANDING");

    execute(
        "UPDATE penalty.passenger_due SET status = 'SETTLED', settled_at = now(),"
            + " settled_booking_id = "
            + f.bookingId
            + " WHERE passenger_due_id = "
            + dueId);
    assertThat(statusOf(dueId)).isEqualTo("SETTLED");
  }

  @Test
  @DisplayName("An outstanding due may not carry a settlement timestamp, and vice versa")
  void statusAndSettlementCannotDisagree() throws Exception {
    Fixture f = seed("dues-status-pair");
    long penaltyId = insertPenalty(f, "PASSENGER_NO_SHOW", "49.00", "25.00", "24.00", "DUES");
    long dueId = insertDue(f, penaltyId, "49.00");

    assertThatThrownBy(
            () ->
                execute(
                    "UPDATE penalty.passenger_due SET settled_at = now() WHERE passenger_due_id = "
                        + dueId))
        .hasMessageContaining("passenger_due_settlement_pair");
  }

  @Test
  @DisplayName("A split that does not re-add to the fee cannot be stored at all")
  void theSplitMustAlwaysReAddToTheFee() throws Exception {
    Fixture f = seed("split-check");

    assertThatThrownBy(
            () -> insertPenalty(f, "PASSENGER_NO_SHOW", "49.00", "25.00", "25.00", "NETTED"))
        .hasMessageContaining("penalty_assessment_split_adds_up");
  }

  @Test
  @DisplayName("Beneficiary amounts must total the victim half exactly — no rupee created or lost")
  void beneficiaryAmountsMustTotalTheVictimShare() throws Exception {
    Fixture f = seed("beneficiary-total");
    long penaltyId =
        insertPenalty(
            f, "DRIVER_LATE_CANCELLATION", "86.00", "43.00", "43.00", "EARNINGS_DEDUCTION");

    // 22 + 21 = 43, exactly as D31 shares it between two riders.
    try (Connection c = dataSource.getConnection()) {
      c.setAutoCommit(false);
      insertBeneficiary(c, penaltyId, f.passengerAppUserId, f.bookingId, "22.00");
      insertBeneficiary(c, penaltyId, f.driverAppUserId, null, "21.00");
      c.commit();
    }
    assertThat(beneficiaryTotal(penaltyId)).isEqualTo("43.00");

    // One rupee short, and the deferred trigger refuses the whole transaction at commit.
    Fixture g = seed("beneficiary-short");
    long shortPenalty =
        insertPenalty(
            g, "DRIVER_LATE_CANCELLATION", "86.00", "43.00", "43.00", "EARNINGS_DEDUCTION");
    assertThatThrownBy(
            () -> {
              try (Connection c = dataSource.getConnection()) {
                c.setAutoCommit(false);
                insertBeneficiary(c, shortPenalty, g.passengerAppUserId, g.bookingId, "22.00");
                insertBeneficiary(c, shortPenalty, g.driverAppUserId, null, "20.00");
                c.commit();
              }
            })
        .hasMessageContaining("victim share");
  }

  @Test
  @DisplayName("Twenty simultaneous triggers assess one penalty; nineteen are refused by the index")
  void concurrentTriggersAssessExactlyOnce() throws Exception {
    Fixture f = seed("concurrent-assessment");

    int threads = 20;
    CyclicBarrier allAtOnce = new CyclicBarrier(threads);
    AtomicInteger admitted = new AtomicInteger();
    AtomicInteger refused = new AtomicInteger();

    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      Callable<Void> assess =
          () -> {
            allAtOnce.await(30, TimeUnit.SECONDS);
            try {
              insertPenalty(f, "PASSENGER_NO_SHOW", "49.00", "25.00", "24.00", "NETTED");
              admitted.incrementAndGet();
            } catch (SQLException duplicate) {
              assertThat(duplicate.getSQLState()).isEqualTo("23505");
              refused.incrementAndGet();
            }
            return null;
          };
      for (Future<Void> future : pool.invokeAll(Collections.nCopies(threads, assess))) {
        future.get(60, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
    }

    assertThat(admitted.get()).isEqualTo(1);
    assertThat(refused.get()).isEqualTo(threads - 1);
  }

  @Test
  @DisplayName("Only one dispute per penalty may be open at a time")
  void onlyOneOpenDisputePerPenalty() throws Exception {
    Fixture f = seed("one-open-dispute");
    long penaltyId = insertPenalty(f, "PASSENGER_NO_SHOW", "49.00", "25.00", "24.00", "NETTED");

    insertDispute(penaltyId, f.passengerAppUserId, "OPEN");
    assertThatThrownBy(() -> insertDispute(penaltyId, f.passengerAppUserId, "OPEN"))
        .hasMessageContaining("penalty_dispute_open_uk");

    // A decided one does not block the record of what was argued before it.
    insertDispute(penaltyId, f.passengerAppUserId, "UPHELD");
  }

  // ── fixtures ─────────────────────────────────────────────────────────────────────────────────

  private record Fixture(
      long driverAppUserId, long passengerAppUserId, long bookingId, long tripId) {}

  private Fixture seed(String key) throws SQLException {
    try (Connection c = dataSource.getConnection()) {
      long driverAppUserId =
          id(
              c,
              "INSERT INTO identity.app_user(keycloak_subject, display_name) VALUES ('drv-"
                  + key
                  + "', 'Priya Jayawardena') RETURNING app_user_id");
      long passengerAppUserId =
          id(
              c,
              "INSERT INTO identity.app_user(keycloak_subject, display_name) VALUES ('pax-"
                  + key
                  + "', 'Dinuka Silva') RETURNING app_user_id");
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
                  + ", TIMESTAMPTZ '2026-08-02 09:00:00+00', 3) RETURNING route_occurrence_id");
      long tripId =
          id(
              c,
              "INSERT INTO trip.trip(route_plan_id, route_occurrence_id, status) VALUES ("
                  + routePlanId
                  + ", "
                  + occurrenceId
                  + ", 'SCHEDULED') RETURNING trip_id");
      long bookingId =
          id(
              c,
              "INSERT INTO booking.booking(route_plan_id, route_occurrence_id,"
                  + " passenger_app_user_id, seats, status, fare_estimate, pickup, dropoff) VALUES ("
                  + routePlanId
                  + ", "
                  + occurrenceId
                  + ", "
                  + passengerAppUserId
                  + ", 1, 'CONFIRMED', 197.00,"
                  + " ST_SetSRID(ST_MakePoint(79.86, 6.93), 4326),"
                  + " ST_SetSRID(ST_MakePoint(79.88, 6.90), 4326)) RETURNING booking_id");
      return new Fixture(driverAppUserId, passengerAppUserId, bookingId, tripId);
    }
  }

  private long insertPenalty(
      Fixture f,
      String kind,
      String fee,
      String victimShare,
      String platformShare,
      String collection)
      throws SQLException {
    try (Connection c = dataSource.getConnection()) {
      return id(
          c,
          "INSERT INTO penalty.penalty_assessment(kind, booking_id, trip_id, payer_app_user_id,"
              + " payer_role, victim_role, fare_base, percent, fee_amount, victim_share,"
              + " platform_share, status, collection_method, explanation, policy_version) VALUES ('"
              + kind
              + "', "
              + f.bookingId
              + ", "
              + f.tripId
              + ", "
              + f.passengerAppUserId
              + ", 'PASSENGER', 'DRIVER', 197.00, 25.00, "
              + fee
              + ", "
              + victimShare
              + ", "
              + platformShare
              + ", 'SETTLED', '"
              + collection
              + "', 'explained', 'v1') RETURNING penalty_id");
    }
  }

  private long insertDue(Fixture f, long penaltyId, String amount) throws SQLException {
    try (Connection c = dataSource.getConnection()) {
      return id(
          c,
          "INSERT INTO penalty.passenger_due(app_user_id, penalty_id, amount, reason,"
              + " origin_booking_id, status) VALUES ("
              + f.passengerAppUserId
              + ", "
              + penaltyId
              + ", "
              + amount
              + ", 'No-show fee', "
              + f.bookingId
              + ", 'OUTSTANDING') RETURNING passenger_due_id");
    }
  }

  private void insertBeneficiary(
      Connection c, long penaltyId, long appUserId, Long bookingId, String amount)
      throws SQLException {
    try (PreparedStatement ps =
        c.prepareStatement(
            "INSERT INTO penalty.penalty_beneficiary(penalty_id, beneficiary_app_user_id,"
                + " booking_id, amount) VALUES (?, ?, ?, ?::numeric)")) {
      ps.setLong(1, penaltyId);
      ps.setLong(2, appUserId);
      if (bookingId == null) {
        ps.setNull(3, java.sql.Types.BIGINT);
      } else {
        ps.setLong(3, bookingId);
      }
      ps.setString(4, amount);
      ps.executeUpdate();
    }
  }

  private void insertDispute(long penaltyId, long appUserId, String status) throws SQLException {
    try (Connection c = dataSource.getConnection()) {
      execute(
          c,
          "INSERT INTO penalty.penalty_dispute(penalty_id, raised_by_app_user_id, reason, status"
              + (status.equals("OPEN") ? "" : ", decided_at")
              + ") VALUES ("
              + penaltyId
              + ", "
              + appUserId
              + ", 'I was there', '"
              + status
              + "'"
              + (status.equals("OPEN") ? "" : ", now()")
              + ")");
    }
  }

  private String beneficiaryTotal(long penaltyId) throws SQLException {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT sum(amount)::text FROM penalty.penalty_beneficiary WHERE penalty_id = ?")) {
      ps.setLong(1, penaltyId);
      try (ResultSet rs = ps.executeQuery()) {
        assertThat(rs.next()).isTrue();
        return rs.getString(1);
      }
    }
  }

  private String statusOf(long dueId) throws SQLException {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT status FROM penalty.passenger_due WHERE passenger_due_id = ?")) {
      ps.setLong(1, dueId);
      try (ResultSet rs = ps.executeQuery()) {
        assertThat(rs.next()).isTrue();
        return rs.getString(1);
      }
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
