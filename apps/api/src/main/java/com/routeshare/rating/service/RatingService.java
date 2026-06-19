package com.routeshare.rating.service;

import com.routeshare.rating.dto.RateBookingRequest;
import com.routeshare.rating.dto.RatingResponse;
import com.routeshare.rating.dto.RatingSummaryResponse;

public interface RatingService {
  /** Passenger rates the driver of a completed booking they own. */
  RatingResponse ratePassengerBooking(long bookingId, RateBookingRequest req);

  /** Ratings received by the current driver, with average + count. */
  RatingSummaryResponse myDriverRatings();
}
