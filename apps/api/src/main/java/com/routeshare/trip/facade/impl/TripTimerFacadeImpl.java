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

  @Override
  public PickupWaitResponse pickupWindowForBooking(long bookingId) {
    return pickupWaits.passengerWindow(bookingId);
  }
}
