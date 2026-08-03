package com.routeshare.location.dto.response;

public record ApproachResponse(
    boolean active,
    PickupPoint pickupPoint,
    Position counterparty,
    Double distanceMeters,
    Long etaSeconds,
    Vehicle vehicle) {
  public record PickupPoint(
      String label, String description, String sideHint, double latitude, double longitude) {}

  public record Position(double latitude, double longitude, long updatedSecondsAgo) {}

  public record Vehicle(String make, String colour, String plate) {}

  public static ApproachResponse inactive() {
    return new ApproachResponse(false, null, null, null, null, null);
  }
}
