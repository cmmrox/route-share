package com.routeshare.rewards.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReferralResponse(
    String code,
    String link,
    int invited,
    int joined,
    int stillEarning,
    BigDecimal totalEarned,
    BigDecimal passengerRatePct,
    BigDecimal driverRatePct,
    int windowMonths,
    int maxTrips,
    BigDecimal refereeFirstRideDiscount,
    List<Row> rows) {
  public record Row(
      String who,
      String role,
      LocalDate joinedAt,
      int trips,
      int tripsLeft,
      BigDecimal earned,
      String status) {}
}
