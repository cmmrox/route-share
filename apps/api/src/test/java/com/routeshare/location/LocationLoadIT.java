package com.routeshare.location;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import com.routeshare.location.domain.*;
import java.time.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class LocationLoadIT {
  @Test
  void threeHundredConcurrentProgressRowsStayInsideInProcessBudget() {
    var eta = new EtaCalculator(22);
    assertTimeout(
        Duration.ofMillis(50),
        () ->
            IntStream.range(0, 300).mapToObj(i -> eta.etaSeconds(10_000 - (i * 10), 8.0)).toList());
  }
}
