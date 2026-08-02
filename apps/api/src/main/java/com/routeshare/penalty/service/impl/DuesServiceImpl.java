package com.routeshare.penalty.service.impl;

import com.routeshare.payment.facade.PaymentFacade;
import com.routeshare.penalty.dto.response.AppliedDuesResponse;
import com.routeshare.penalty.dto.response.DuesResponse;
import com.routeshare.penalty.entity.PassengerDueEntity;
import com.routeshare.penalty.repository.PassengerDueRepository;
import com.routeshare.penalty.repository.PenaltyAssessmentRepository;
import com.routeshare.penalty.service.DuesService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DuesServiceImpl implements DuesService {
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

  private final PassengerDueRepository dues;
  private final PenaltyAssessmentRepository penalties;
  private final PaymentFacade payments;
  private final Clock clock;

  /**
   * Held rather than re-read on every scrape: the gauge is refreshed whenever a due is written or
   * settled, which is the only time it can change, and a rising value means cash penalties are not
   * being recovered.
   */
  private final AtomicReference<BigDecimal> outstandingGauge = new AtomicReference<>(ZERO);

  public DuesServiceImpl(
      PassengerDueRepository dues,
      PenaltyAssessmentRepository penalties,
      PaymentFacade payments,
      MeterRegistry meters,
      Clock clock) {
    this.dues = dues;
    this.penalties = penalties;
    this.payments = payments;
    this.clock = clock;
    Gauge.builder(
            "routeshare_dues_outstanding_amount", outstandingGauge, v -> v.get().doubleValue())
        .description("Penalty fees assessed against cash passengers and not yet recovered")
        .register(meters);
  }

  @Override
  @Transactional(readOnly = true)
  public DuesResponse dues(long appUserId) {
    List<PassengerDueEntity> rows = dues.findByAppUserIdOrderByIdDesc(appUserId);
    List<DuesResponse.Item> items = rows.stream().map(this::toItem).toList();
    BigDecimal total =
        rows.stream()
            .filter(PassengerDueEntity::isOutstanding)
            .map(PassengerDueEntity::getAmount)
            .reduce(ZERO, BigDecimal::add);
    // P25b is an empty state with a reason, not an empty list: "nothing owed" and "nothing loaded"
    // must not render the same.
    return new DuesResponse(items, total, total.signum() == 0);
  }

  @Override
  @Transactional
  public void recordDue(
      long appUserId, long penaltyId, BigDecimal amount, String reason, Long bookingId) {
    if (amount == null || amount.signum() <= 0) {
      return;
    }
    if (dues.findByPenaltyId(penaltyId).isPresent()) {
      // One due per penalty. A retried assessment must not bill the same no-show twice.
      return;
    }
    dues.save(PassengerDueEntity.outstanding(appUserId, penaltyId, amount, reason, bookingId));
    refreshGauge();
  }

  @Override
  @Transactional
  public AppliedDuesResponse applyOutstandingDues(long appUserId, long bookingId) {
    List<PassengerDueEntity> outstanding = dues.findOutstanding(appUserId);
    if (outstanding.isEmpty()) {
      return AppliedDuesResponse.empty();
    }
    List<AppliedDuesResponse.Line> lines = new ArrayList<>(outstanding.size());
    BigDecimal total = ZERO;
    for (PassengerDueEntity due : outstanding) {
      // Attached, not settled. The money only moves when this booking's card is captured, so a
      // booking that never starts leaves the fee outstanding rather than quietly clearing it.
      due.carriedBy(bookingId);
      lines.add(
          new AppliedDuesResponse.Line(
              due.getId(), due.getReason(), tripLabel(due), due.getCreatedAt(), due.getAmount()));
      total = total.add(due.getAmount());
    }
    dues.saveAll(outstanding);
    return new AppliedDuesResponse(List.copyOf(lines), total);
  }

  @Override
  @Transactional
  public void settleDuesForBooking(long bookingId) {
    List<PassengerDueEntity> carried =
        dues.findBySettledBookingId(bookingId).stream()
            .filter(PassengerDueEntity::isOutstanding)
            .toList();
    if (carried.isEmpty()) {
      return;
    }
    BigDecimal total = ZERO;
    for (PassengerDueEntity due : carried) {
      due.settle(bookingId, clock.instant());
      total = total.add(due.getAmount());
    }
    dues.saveAll(carried);
    payments.recordDuesSettlement(bookingId, total);
    refreshGauge();
    log.info("dues of {} settled by booking {}", total, bookingId);
  }

  @Override
  @Transactional
  public void releaseDuesForBooking(long bookingId) {
    List<PassengerDueEntity> carried =
        dues.findBySettledBookingId(bookingId).stream()
            .filter(PassengerDueEntity::isOutstanding)
            .toList();
    if (carried.isEmpty()) {
      return;
    }
    carried.forEach(PassengerDueEntity::release);
    dues.saveAll(carried);
  }

  @Override
  @Transactional
  public void waiveDueForPenalty(long penaltyId) {
    dues.findByPenaltyId(penaltyId)
        .ifPresent(
            due -> {
              due.waive(clock.instant());
              dues.save(due);
              refreshGauge();
            });
  }

  private DuesResponse.Item toItem(PassengerDueEntity due) {
    return new DuesResponse.Item(
        due.getId(),
        due.getReason(),
        penalties
            .findById(due.getPenaltyId())
            .map(p -> p.getPercent().stripTrailingZeros().toPlainString() + "% of the fare")
            .orElse("A fee from an earlier trip"),
        due.getCreatedAt(),
        tripLabel(due),
        due.getAmount(),
        "cash",
        due.getStatus(),
        due.getOriginBookingId(),
        due.getSettledBookingId());
  }

  private String tripLabel(PassengerDueEntity due) {
    if (due.getOriginBookingId() == null) {
      return null;
    }
    return penalties
        .findTripLabelsForBooking(due.getOriginBookingId())
        .map(row -> row.getOriginLabel() + " → " + row.getDestinationLabel())
        .orElse(null);
  }

  private void refreshGauge() {
    outstandingGauge.set(dues.sumOutstanding());
  }
}
