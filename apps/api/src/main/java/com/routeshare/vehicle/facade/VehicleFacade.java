package com.routeshare.vehicle.facade;

public interface VehicleFacade {
  boolean existsApprovedOwnedVehicleWithCapacity(long vehicleId, long driverProfileId, int seats);

  boolean existsByVehicleIdAndDriverProfileId(long vehicleId, long driverProfileId);

  /** True when the driver has at least one APPROVED vehicle (used for verification readiness). */
  boolean existsApprovedVehicleForDriver(long driverProfileId);

  /**
   * True when the driver has an approved vehicle whose rate band is live and priced.
   *
   * <p>Approved papers are not enough to publish: without a band there is no legal price to put on
   * a seat (board D40). The publish gate asks this, and slice 03's fare engine reads the rate.
   */
  boolean existsPublishableVehicleForDriver(long driverProfileId);

  /** The per-km rate the driver chose for this vehicle, empty when no live band is set. */
  java.util.Optional<java.math.BigDecimal> ratePerKmFor(long vehicleId);

  boolean hasActiveBand(long vehicleId);
}
