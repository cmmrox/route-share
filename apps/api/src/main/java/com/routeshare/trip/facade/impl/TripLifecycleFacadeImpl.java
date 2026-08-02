package com.routeshare.trip.facade.impl;

import com.routeshare.trip.facade.TripLifecycleFacade;
import com.routeshare.trip.service.TripLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TripLifecycleFacadeImpl implements TripLifecycleFacade {

  private final TripLifecycleService lifecycle;
  private final com.routeshare.trip.service.DriverLateGraceService graces;

  @Override
  public long ensureTripForBookedOccurrence(long routeOccurrenceId) {
    return lifecycle.ensureTripForBookedOccurrence(routeOccurrenceId);
  }

  @Override
  public void openLateGraceForBooking(long bookingId) {
    graces.openForBooking(bookingId);
  }

  @Override
  public void resolveLateGraceOnCancel(long bookingId) {
    graces.resolveCancelled(bookingId);
  }
}
