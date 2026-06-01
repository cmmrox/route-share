package com.routeshare.routing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.routeshare.routing.dto.request.CoordinateRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteBucketCellGeneratorTest {
  @Test
  void createsStableDeduplicatedBucketCellsForRouteCoordinates() {
    var generator = new RouteBucketCellGenerator();

    var cells =
        generator.cellsFor(
            List.of(
                new CoordinateRequest(6.9271, 79.8612),
                new CoordinateRequest(6.9271, 79.8612),
                new CoordinateRequest(7.2906, 80.6337)),
            3);

    assertThat(cells).containsExactly("r3:069:798", "r3:072:806");
  }
}
