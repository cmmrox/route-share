package com.routeshare.passenger.dto.response;

/**
 * P02's dashboard card.
 *
 * <p>{@code liveMatchCount} and {@code bestMatch} come from the ordinary search path run over the
 * saved pair — reused rather than duplicated, so the number on the dashboard and the list she sees
 * when she taps it cannot disagree.
 */
public record UsualCommuteResponse(
    boolean saved,
    String originLabel,
    Double originLatitude,
    Double originLongitude,
    String destinationLabel,
    Double destinationLatitude,
    Double destinationLongitude,
    String habitualTime,
    int liveMatchCount,
    BestMatch bestMatch) {

  public record BestMatch(
      long routeOccurrenceId,
      String driverName,
      double matchPercent,
      String matchTier,
      java.math.BigDecimal price,
      java.time.Instant departsAt) {}

  public static UsualCommuteResponse none() {
    return new UsualCommuteResponse(false, null, null, null, null, null, null, null, 0, null);
  }
}
