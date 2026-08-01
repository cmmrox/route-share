package com.routeshare.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
 * Slice 04 said double capture is impossible. The unit tests assert that against mocks, which can
 * only show that the code takes the right branch — the thing that actually makes it impossible is
 * the unique index on {@code payment.payment_attempt.idempotency_key}, and an index can only be
 * proven by a real database under real contention.
 *
 * <p>This is the test Blocker 013 recorded as unwritable while no database could be started.
 */
@Testcontainers(disabledWithoutDocker = true)
class CaptureOnTripStartIT {
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

  /**
   * Twenty threads start the same trip at the same instant. Exactly one may write the capture
   * attempt; the other nineteen must be refused by the database, not by a check that happened to
   * run first.
   */
  @Test
  void onlyOneOfManySimultaneousCapturesForTheSameBookingIsAdmitted() throws Exception {
    long intentId = seedIntent("booking-concurrent-capture");
    String key = "capture:booking:" + intentId;

    int threads = 20;
    CyclicBarrier allAtOnce = new CyclicBarrier(threads);
    AtomicInteger admitted = new AtomicInteger();
    AtomicInteger refused = new AtomicInteger();

    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      Callable<Void> capture =
          () -> {
            allAtOnce.await(30, TimeUnit.SECONDS);
            try (Connection c = dataSource.getConnection();
                PreparedStatement ps =
                    c.prepareStatement(
                        """
                        INSERT INTO payment.payment_attempt
                            (payment_intent_id, operation, idempotency_key, amount, status)
                        VALUES (?, 'CAPTURE', ?, 428.00, 'SUCCEEDED')
                        """)) {
              ps.setLong(1, intentId);
              ps.setString(2, key);
              ps.executeUpdate();
              admitted.incrementAndGet();
            } catch (SQLException duplicate) {
              // 23505 unique_violation — the index, not the application, refused this one.
              assertThat(duplicate.getSQLState()).isEqualTo("23505");
              refused.incrementAndGet();
            }
            return null;
          };

      for (Future<Void> f : pool.invokeAll(java.util.Collections.nCopies(threads, capture))) {
        f.get(60, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
    }

    assertThat(admitted.get()).isEqualTo(1);
    assertThat(refused.get()).isEqualTo(threads - 1);
    assertThat(countAttempts(key)).isEqualTo(1);
  }

  /** A void and a capture are different operations, so they must not collide on the same key. */
  @Test
  void differentOperationsOnTheSameBookingDoNotCollide() throws Exception {
    long intentId = seedIntent("booking-distinct-operations");
    insertAttempt(intentId, "CAPTURE", "capture:booking:" + intentId);
    insertAttempt(intentId, "VOID", "void:booking:" + intentId);

    assertThat(countAttempts("capture:booking:" + intentId)).isEqualTo(1);
    assertThat(countAttempts("void:booking:" + intentId)).isEqualTo(1);
  }

  private long seedIntent(String reference) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                """
                INSERT INTO payment.payment_intent (amount, currency, status, provider_reference)
                VALUES (428.00, 'LKR', 'AUTHORIZED', ?)
                RETURNING payment_intent_id
                """)) {
      ps.setString(1, reference);
      try (ResultSet rs = ps.executeQuery()) {
        assertThat(rs.next()).isTrue();
        return rs.getLong(1);
      }
    }
  }

  private void insertAttempt(long intentId, String operation, String key) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                """
                INSERT INTO payment.payment_attempt
                    (payment_intent_id, operation, idempotency_key, amount, status)
                VALUES (?, ?, ?, 428.00, 'SUCCEEDED')
                """)) {
      ps.setLong(1, intentId);
      ps.setString(2, operation);
      ps.setString(3, key);
      ps.executeUpdate();
    }
  }

  private int countAttempts(String key) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT count(*) FROM payment.payment_attempt WHERE idempotency_key = ?")) {
      ps.setString(1, key);
      try (ResultSet rs = ps.executeQuery()) {
        assertThat(rs.next()).isTrue();
        return rs.getInt(1);
      }
    }
  }
}
