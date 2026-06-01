package com.routeshare.payment.repository;

import com.routeshare.payment.entity.PaymentIntentEntity;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntentEntity, Long> {
  Optional<PaymentIntentEntity> findFirstByBookingIdAndStatusInOrderByIdDesc(
      long bookingId, java.util.Collection<String> statuses);

  default Optional<PaymentIntentView> findActiveByBookingId(long bookingId) {
    return findFirstByBookingIdAndStatusInOrderByIdDesc(
            bookingId, java.util.List.of("REQUIRES_CAPTURE", "CAPTURED"))
        .map(this::toView);
  }

  default PaymentIntentView create(
      long bookingId, String providerReference, BigDecimal amount, String currency) {
    return toView(
        save(
            new PaymentIntentEntity(
                null, bookingId, null, providerReference, amount, currency, null)));
  }

  private PaymentIntentView toView(PaymentIntentEntity entity) {
    return new PaymentIntentView(
        entity.getProvider(),
        entity.getProviderReference(),
        entity.getStatus(),
        entity.getAmount(),
        entity.getCurrency());
  }

  record PaymentIntentView(
      String provider,
      String providerReference,
      String status,
      BigDecimal amount,
      String currency) {}
}
