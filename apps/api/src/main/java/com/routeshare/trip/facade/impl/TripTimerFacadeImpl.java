package com.routeshare.trip.facade.impl;

import com.routeshare.trip.dto.response.PickupWaitResponse;
import com.routeshare.trip.facade.TripTimerFacade;
import com.routeshare.trip.service.PickupWaitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TripTimerFacadeImpl implements TripTimerFacade {

  private final PickupWaitService pickupWaits;
  private final com.routeshare.trip.service.DriverLateGraceService graces;

  @Override
  public PickupWaitResponse pickupWindowForBooking(long bookingId) {
    return pickupWaits.passengerWindow(bookingId);
  }

  @Override
  public com.routeshare.trip.dto.response.CancellationTermsResponse cancellationTerms(
      long bookingId) {
    return graces.cancellationTerms(bookingId);
  }
}
