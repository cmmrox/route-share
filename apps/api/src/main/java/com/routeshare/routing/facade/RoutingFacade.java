package com.routeshare.routing.facade;

import java.util.Optional;

public interface RoutingFacade {
  Optional<RouteReservation> reserveSeatsAndReturnRouteLength(long routeOccurrenceId, int seats);

  /**
   * The vehicle and route length behind a published trip — everything pricing needs, nothing more.
   */
  Optional<PriceableTrip> findPriceableTrip(long routeOccurrenceId);

  /**
   * Returns seats to an occurrence's inventory. Called when a seat stops being held — a no-show
   * release — so the car is not driven with a seat nobody can book.
   */
  void releaseSeats(long routeOccurrenceId, int seats);

  /**
   * Withdraws a driver's future offers when driving is deactivated.
   *
   * <p>Leaving them published would let riders keep booking seats in a car that is no longer
   * allowed to carry them, and the first anybody would hear of it is at the kerb. Occurrences that
   * have already departed are left alone — those trips happened.
   *
   * @return how many future occurrences were withdrawn
   */
  int cancelFutureOccurrencesForDriver(long driverProfileId);

  record PriceableTrip(long routeOccurrenceId, long vehicleId, double routeLengthMeters) {}
}
