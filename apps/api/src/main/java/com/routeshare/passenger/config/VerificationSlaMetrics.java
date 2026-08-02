package com.routeshare.passenger.config;

import com.routeshare.passenger.repository.VerificationSessionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * How many riders have been waiting longer than the review SLA.
 *
 * <p>Gauged rather than counted: a large queue that is moving is fine and a small one that is not
 * is not, and only the age tells the two apart. A rider stuck at {@code PENDING} sees no verified
 * trips and gets no explanation, and nothing else in the system will notice on her behalf.
 */
@Component
public class VerificationSlaMetrics {

  private final VerificationSessionRepository sessions;
  private final Clock clock;
  private final Duration sla;

  public VerificationSlaMetrics(
      VerificationSessionRepository sessions,
      MeterRegistry meters,
      Clock clock,
      @Value("${routeshare.verification.review-sla-hours:24}") long slaHours) {
    this.sessions = sessions;
    this.clock = clock;
    this.sla = Duration.ofHours(slaHours);
    meters.gauge("routeshare_verification_pending_over_sla", this, VerificationSlaMetrics::overSla);
  }

  private double overSla() {
    return sessions.countPendingOlderThan(clock.instant().minus(sla));
  }
}
