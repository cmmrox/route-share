package com.routeshare.common.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class NativeQueryMapper {
  private NativeQueryMapper() {}

  @SuppressWarnings("unchecked")
  public static <T> Optional<T> single(List<?> rows, Class<T> type) {
    return rows.stream().findFirst().map(type::cast);
  }

  public static long longValue(Object value) {
    return ((Number) value).longValue();
  }

  public static int intValue(Object value) {
    return ((Number) value).intValue();
  }

  public static double doubleValue(Object value) {
    return ((Number) value).doubleValue();
  }

  public static Instant instant(Object value) {
    return value instanceof Timestamp ts ? ts.toInstant() : (Instant) value;
  }
}
