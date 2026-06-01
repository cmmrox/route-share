package com.routeshare.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class VirtualThreadConfigurationTest {
  private static final Path APPLICATION_YML = Path.of("src/main/resources/application.yml");

  @Test
  void springVirtualThreadsAreEnabled() throws IOException {
    String yaml = Files.readString(APPLICATION_YML);

    assertThat(yaml).contains("threads:", "virtual:", "enabled: true");
  }

  @Test
  void databasePoolIsBoundedWhenUsingVirtualThreads() throws IOException {
    String yaml = Files.readString(APPLICATION_YML);

    assertThat(yaml)
        .contains(
            "hikari:",
            "maximum-pool-size: ${ROUTESHARE_DB_POOL_MAX_SIZE:20}",
            "minimum-idle: ${ROUTESHARE_DB_POOL_MIN_IDLE:5}");
  }
}
