package com.routeshare.rating.repository;

import com.routeshare.rating.entity.RatingEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatingRepository extends JpaRepository<RatingEntity, Long> {
  boolean existsByBookingIdAndRaterAppUserId(long bookingId, long raterAppUserId);

  List<RatingEntity> findByRateeAppUserIdOrderByIdDesc(long rateeAppUserId);

  @Query(
      "select coalesce(avg(r.stars), 0) as average, count(r) as count from RatingEntity r"
          + " where r.rateeAppUserId = :rateeAppUserId")
  RateeAggregate aggregateForRatee(@Param("rateeAppUserId") long rateeAppUserId);

  interface RateeAggregate {
    Double getAverage();

    long getCount();
  }
}
