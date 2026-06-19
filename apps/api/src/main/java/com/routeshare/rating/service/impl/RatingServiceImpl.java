package com.routeshare.rating.service.impl;

import com.routeshare.booking.facade.BookingFacade;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.rating.dto.RateBookingRequest;
import com.routeshare.rating.dto.RatingResponse;
import com.routeshare.rating.dto.RatingSummaryResponse;
import com.routeshare.rating.entity.RatingEntity;
import com.routeshare.rating.repository.RatingRepository;
import com.routeshare.rating.service.RatingService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final BookingFacade bookingFacade;
  private final RatingRepository ratings;
  private final NotificationFacade notifications;

  @Override
  @Transactional
  public RatingResponse ratePassengerBooking(long bookingId, RateBookingRequest req) {
    long raterAppUserId = currentAppUserId();
    Long driverAppUserId =
        bookingFacade
            .findDriverAppUserIdForPassengerBooking(bookingId, raterAppUserId)
            .orElseThrow(
                () -> new AccessDeniedException("Booking does not belong to current user"));
    if (ratings.existsByBookingIdAndRaterAppUserId(bookingId, raterAppUserId)) {
      throw new IllegalStateException("You have already rated this trip");
    }
    var saved =
        ratings.save(
            RatingEntity.create(
                bookingId,
                raterAppUserId,
                driverAppUserId,
                "PASSENGER",
                req.stars(),
                req.comment()));
    notifications.notifyUser(
        driverAppUserId,
        "RATING_RECEIVED",
        "You received a new rating",
        req.stars() + "-star rating from a passenger",
        Map.of("bookingId", String.valueOf(bookingId), "stars", String.valueOf(req.stars())));
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public RatingSummaryResponse myDriverRatings() {
    long driverAppUserId = currentAppUserId();
    var agg = ratings.aggregateForRatee(driverAppUserId);
    double avg = agg == null || agg.getAverage() == null ? 0.0 : agg.getAverage();
    long count = agg == null ? 0 : agg.getCount();
    List<RatingResponse> recent =
        ratings.findByRateeAppUserIdOrderByIdDesc(driverAppUserId).stream()
            .limit(20)
            .map(this::toResponse)
            .toList();
    return new RatingSummaryResponse(Math.round(avg * 100.0) / 100.0, count, recent);
  }

  private long currentAppUserId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }

  private RatingResponse toResponse(RatingEntity e) {
    return new RatingResponse(
        e.getId(), e.getBookingId(), e.getStars(), e.getComment(), e.getCreatedAt());
  }
}
