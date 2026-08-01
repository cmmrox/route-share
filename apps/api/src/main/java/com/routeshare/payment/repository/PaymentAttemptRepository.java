package com.routeshare.payment.repository;

import com.routeshare.payment.entity.PaymentAttemptEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttemptEntity, Long> {
  Optional<PaymentAttemptEntity> findByIdempotencyKey(String idempotencyKey);

  List<PaymentAttemptEntity> findByPaymentIntentIdOrderByIdDesc(long paymentIntentId);

  /** Attempts that started and never finished — a gateway timeout, or a crash mid-call. */
  List<PaymentAttemptEntity> findByStatusOrderByIdAsc(String status);
}
