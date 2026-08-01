package com.routeshare.payment.service.impl;

import com.routeshare.payment.domain.PaymentIntentStatus;
import com.routeshare.payment.entity.PaymentAttemptEntity;
import com.routeshare.payment.repository.PaymentAttemptRepository;
import com.routeshare.payment.repository.PaymentIntentRepository;
import com.routeshare.payment.service.PaymentReconciliationService;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class PaymentReconciliationServiceImpl implements PaymentReconciliationService {
  private final PaymentIntentRepository intents;
  private final PaymentAttemptRepository attempts;
  private final Clock clock;
  private final Duration authValidity;

  public PaymentReconciliationServiceImpl(
      PaymentIntentRepository intents,
      PaymentAttemptRepository attempts,
      MeterRegistry meters,
      Clock clock,
      @Value("${routeshare.payment.auth-validity-hours:168}") long authValidityHours) {
    this.intents = intents;
    this.attempts = attempts;
    this.clock = clock;
    this.authValidity = Duration.ofHours(authValidityHours);
    // Alerting on this gauge is the point: a stuck authorisation is somebody's rent held hostage,
    // and nobody involved will notice on their own.
    meters.gauge("routeshare_payment_stuck_total", this, self -> self.findStuck().size());
  }

  @Override
  @Transactional(readOnly = true)
  public List<StuckPayment> findStuck() {
    List<StuckPayment> stuck = new ArrayList<>();

    intents
        .findByStatusInAndCreatedAtBefore(
            List.of(PaymentIntentStatus.PENDING.name(), PaymentIntentStatus.AUTHORIZED.name()),
            clock.instant().minus(authValidity))
        .forEach(
            intent ->
                stuck.add(
                    new StuckPayment(
                        "STUCK_AUTHORIZATION",
                        intent.getId(),
                        intent.getBookingId(),
                        intent.getStatus(),
                        intent.getAmount(),
                        intent.getCreatedAt(),
                        intent.getProviderReference())));

    attempts
        .findByStatusOrderByIdAsc(PaymentAttemptEntity.STARTED)
        .forEach(
            attempt ->
                stuck.add(
                    new StuckPayment(
                        "UNFINISHED_ATTEMPT",
                        attempt.getPaymentIntentId(),
                        attempt.getBookingId(),
                        attempt.getOperation(),
                        attempt.getAmount(),
                        attempt.getStartedAt(),
                        attempt.getProviderReference())));

    return List.copyOf(stuck);
  }
}
