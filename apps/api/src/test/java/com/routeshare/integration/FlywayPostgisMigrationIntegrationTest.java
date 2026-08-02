package com.routeshare.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class FlywayPostgisMigrationIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(
              DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("routeshare")
          .withUsername("routeshare")
          .withPassword("routeshare_dev_password");

  @Test
  void flywayMigrationsCreatePostgisSchemasAndPrePhase06Tables() throws Exception {
    Flyway flyway =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .load();

    flyway.migrate();

    try (Connection connection =
        DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
      assertThat(exists(connection, "SELECT 1 FROM pg_extension WHERE extname = 'postgis'"))
          .isTrue();
      assertThat(
              exists(
                  connection,
                  "SELECT 1 FROM information_schema.schemata WHERE schema_name = 'booking'"))
          .isTrue();
      assertThat(
              exists(
                  connection,
                  "SELECT 1 FROM information_schema.tables WHERE table_schema = 'payment' AND table_name = 'fare_ledger_entry'"))
          .isTrue();
      assertThat(
              exists(
                  connection,
                  "SELECT 1 FROM information_schema.tables WHERE table_schema = 'trip' AND table_name = 'pre_trip_checklist'"))
          .isTrue();
      assertThat(
              exists(
                  connection,
                  "SELECT 1 FROM information_schema.tables WHERE table_schema = 'routing' AND table_name = 'route_share_link'"))
          .isTrue();
      assertThat(latestMigrationVersion(connection)).isEqualTo("035");
    }
  }

  private boolean exists(Connection connection, String sql) throws Exception {
    try (var statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql)) {
      return rs.next();
    }
  }

  private String latestMigrationVersion(Connection connection) throws Exception {
    try (var statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank DESC LIMIT 1")) {
      assertThat(rs.next()).isTrue();
      return rs.getString(1);
    }
  }
}
