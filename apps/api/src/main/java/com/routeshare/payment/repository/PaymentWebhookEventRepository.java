package com.routeshare.payment.repository;

import com.routeshare.payment.entity.PaymentWebhookEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookEventRepository
    extends JpaRepository<PaymentWebhookEventEntity, Long> {
  boolean existsByProviderAndEventId(String provider, String eventId);
}
