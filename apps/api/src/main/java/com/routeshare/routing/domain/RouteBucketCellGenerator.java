package com.routeshare.routing.domain;

import com.routeshare.routing.dto.request.CoordinateRequest;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RouteBucketCellGenerator {
  public Set<String> cellsFor(List<CoordinateRequest> coordinates, int resolution) {
    var cells = new LinkedHashSet<String>();
    for (CoordinateRequest coordinate : coordinates) {
      int latBucket = bucket(coordinate.latitude(), resolution);
      int lngBucket = bucket(coordinate.longitude(), resolution);
      cells.add("r" + resolution + ":" + format(latBucket) + ":" + format(lngBucket));
    }
    return cells;
  }

  public String cellFor(CoordinateRequest coordinate, int resolution) {
    return "r"
        + resolution
        + ":"
        + format(bucket(coordinate.latitude(), resolution))
        + ":"
        + format(bucket(coordinate.longitude(), resolution));
  }

  private int bucket(double value, int resolution) {
    double scale = Math.pow(10, Math.max(0, resolution - 2));
    return (int) Math.floor(value * scale);
  }

  private String format(int value) {
    return String.format("%03d", value);
  }
}
