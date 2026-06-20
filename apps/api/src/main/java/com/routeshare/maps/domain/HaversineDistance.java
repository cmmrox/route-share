package com.routeshare.maps.domain;

/** Great-circle distance, used as the offline fallback when a maps provider is not configured. */
public final class HaversineDistance {
  private static final double EARTH_RADIUS_M = 6_371_000.0;

  private HaversineDistance() {}

  /** Straight-line distance in metres between two WGS84 points. */
  public static long meters(double lat1, double lon1, double lat2, double lon2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return Math.round(EARTH_RADIUS_M * c);
  }
}
