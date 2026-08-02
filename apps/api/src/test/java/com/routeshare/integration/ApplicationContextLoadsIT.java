package com.routeshare.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.routeshare.scheduling.domain.ScheduledJob;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * The application starts.
 *
 * <p>Written after a bean cycle got through a fully green suite and was only found by restarting
 * the API by hand: {@code DriverFacade} gained a write that needed {@code
 * DriverDeactivationService}, which needs identity, which depends back on {@code DriverFacade}.
 * Every unit test passed because every unit test builds its collaborators itself. Nothing in the
 * gate ever asked Spring to wire the graph.
 *
 * <p>It also asserts the scheduled jobs are all discovered, since a job that silently fails to
 * register is a clock nobody runs — and the symptom of that is not an error but a passenger who is
 * never told anything.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationContextLoadsIT {

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(
              DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("routeshare")
          .withUsername("routeshare")
          .withPassword("routeshare_dev_password");

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    // The sweeps are proven by their own tests; leaving them ticking here would race this one.
    registry.add("routeshare.scheduler.enabled", () -> "false");
  }

  @Autowired private List<ScheduledJob> jobs;

  @Test
  void theApplicationContextWiresWithoutACycle() {
    assertThat(jobs).isNotEmpty();
  }

  /** Every clock this slice promised has a job behind it, registered by being a bean. */
  @Test
  void everyClockHasARegisteredJob() {
    assertThat(jobs.stream().map(ScheduledJob::name))
        .contains(
            "start-buffer-expiry",
            "pickup-wait-expiry",
            "driver-late-grace",
            "monthly-counter-reset");
  }

  /** Two jobs sharing a name would share a lock key and quietly stop each other running. */
  @Test
  void jobNamesAreUnique() {
    var names = jobs.stream().map(ScheduledJob::name).toList();
    assertThat(names).doesNotHaveDuplicates();
  }
}
