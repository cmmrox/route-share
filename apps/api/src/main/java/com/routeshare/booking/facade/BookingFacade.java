package com.routeshare.booking.facade;

import java.math.BigDecimal;
import java.util.Optional;

public interface BookingFacade {
  Optional<BigDecimal> findFareEstimateForPassengerBooking(long bookingId, long passengerAppUserId);
}
