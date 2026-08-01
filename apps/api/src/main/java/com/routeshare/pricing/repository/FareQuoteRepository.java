package com.routeshare.pricing.repository;

import com.routeshare.pricing.entity.FareQuoteEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FareQuoteRepository extends JpaRepository<FareQuoteEntity, Long> {
  Optional<FareQuoteEntity> findFirstByBookingIdOrderByIdDesc(long bookingId);
}
