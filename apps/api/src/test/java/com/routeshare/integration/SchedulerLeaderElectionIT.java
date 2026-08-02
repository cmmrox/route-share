package com.routeshare.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Slice 05 done-criterion: the scheduler runs exactly once across instances.
 *
 * <p>Each simulated instance gets its own {@link LockProvider} over the same database — which is
 * what two pods actually are. Asserting this against a single provider would prove only that a
 * local mutex works, and the failure this guards against is specifically the *distributed* one: two
 * instances auto-cancelling the same trip and voiding the same authorisation twice.
 */
@Testcontainers(disabledWithoutDocker = true)
class SchedulerLeaderElectionIT {
  private static final String LOCK_NAME = "routeshare-scheduler-tick";
  private static final String LOCK_TABLE = "scheduling.shedlock";

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

  /** Eight instances tick at the same instant. The job body must execute once. */
  @Test
  void onlyOneInstanceExecutesTheTick() throws Exception {
    int instances = 8;
    CyclicBarrier allAtOnce = new CyclicBarrier(instances);
    AtomicInteger executed = new AtomicInteger();
    AtomicInteger skipped = new AtomicInteger();

    ExecutorService pool = Executors.newFixedThreadPool(instances);
    try {
      Callable<Void> instance =
          () -> {
            LockProvider provider = new JdbcLockProvider(dataSource, LOCK_TABLE);
            allAtOnce.await(30, TimeUnit.SECONDS);
            Optional<SimpleLock> lock = provider.lock(configuration(LOCK_NAME));
            if (lock.isPresent()) {
              executed.incrementAndGet();
              // Held for the duration of the "sweep" — releasing immediately would let a loser
              // acquire it and the test would pass for the wrong reason.
              Thread.sleep(200);
              lock.get().unlock();
            } else {
              skipped.incrementAndGet();
            }
            return null;
          };

      for (Future<Void> f : pool.invokeAll(java.util.Collections.nCopies(instances, instance))) {
        f.get(60, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
    }

    assertThat(executed.get()).isEqualTo(1);
    assertThat(skipped.get()).isEqualTo(instances - 1);
  }

  /**
   * A restart mid-window must not lose the lock's meaning: {@code lockAtMostFor} is what releases a
   * lock held by an instance that died, and until it lapses no other instance may take over.
   */
  @Test
  void aLockHeldByADeadInstanceIsNotStolenBeforeItsBackstopLapses() throws Exception {
    LockProvider first = new JdbcLockProvider(dataSource, LOCK_TABLE);
    LockProvider second = new JdbcLockProvider(dataSource, LOCK_TABLE);

    // Acquired and then abandoned — the instance "died" without unlocking.
    Optional<SimpleLock> held = first.lock(configuration("dead-instance-job"));
    assertThat(held).isPresent();

    assertThat(second.lock(configuration("dead-instance-job"))).isEmpty();
    assertThat(lockUntilIsInTheFuture("dead-instance-job")).isTrue();
  }

  private static LockConfiguration configuration(String name) {
    return new LockConfiguration(Instant.now(), name, Duration.ofMinutes(5), Duration.ofSeconds(0));
  }

  private boolean lockUntilIsInTheFuture(String name) throws Exception {
    try (Connection c = dataSource.getConnection();
        var ps = c.prepareStatement("SELECT lock_until FROM " + LOCK_TABLE + " WHERE name = ?")) {
      ps.setString(1, name);
      try (ResultSet rs = ps.executeQuery()) {
        assertThat(rs.next()).isTrue();
        return rs.getTimestamp(1).toInstant().isAfter(Instant.now());
      }
    }
  }

  /** Guards the assumption the other two tests rest on: the migration really created the table. */
  @Test
  void theShedlockTableExists() throws Exception {
    try (Connection c = dataSource.getConnection();
        var st = c.createStatement();
        ResultSet rs =
            st.executeQuery(
                """
                SELECT column_name FROM information_schema.columns
                 WHERE table_schema = 'scheduling' AND table_name = 'shedlock'
                """)) {
      var columns = new java.util.ArrayList<String>();
      while (rs.next()) {
        columns.add(rs.getString(1));
      }
      assertThat(columns)
          .containsExactlyInAnyOrderElementsOf(
              List.of("name", "lock_until", "locked_at", "locked_by"));
    }
  }
}
