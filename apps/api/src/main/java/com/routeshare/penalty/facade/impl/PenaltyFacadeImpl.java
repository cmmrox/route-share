package com.routeshare.penalty.facade.impl;

import com.routeshare.penalty.domain.PenaltyPolicy;
import com.routeshare.penalty.domain.PenaltySplit;
import com.routeshare.penalty.dto.response.AppliedDuesResponse;
import com.routeshare.penalty.facade.PenaltyFacade;
import com.routeshare.penalty.repository.PenaltyAssessmentRepository;
import com.routeshare.penalty.service.DuesService;
import com.routeshare.penalty.service.PenaltyService;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PenaltyFacadeImpl implements PenaltyFacade {
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

  private final PenaltyService penalties;
  private final DuesService dues;
  private final PenaltyAssessmentRepository assessments;
  private final PolicySettingService policy;

  @Override
  public void assessPassengerNoShow(long bookingId, Long tripId) {
    penalties.assessPassengerNoShow(bookingId, tripId);
  }

  @Override
  public void assessPassengerCancelAfterStart(long bookingId, Long tripId) {
    penalties.assessPassengerCancelAfterStart(bookingId, tripId);
  }

  @Override
  public void assessDriverLate(long bookingId) {
    penalties.assessDriverLate(bookingId);
  }

  @Override
  public void assessDriverLateCancellation(long tripId) {
    penalties.assessDriverLateCancellation(tripId);
  }

  @Override
  public void recordDriverMissedStart(long tripId) {
    penalties.recordDriverMissedStart(tripId);
  }

  @Override
  public AppliedDuesResponse applyOutstandingDues(long appUserId, long bookingId) {
    return dues.applyOutstandingDues(appUserId, bookingId);
  }

  @Override
  public void settleDuesForBooking(long bookingId) {
    dues.settleDuesForBooking(bookingId);
  }

  @Override
  public void releaseDuesForBooking(long bookingId) {
    dues.releaseDuesForBooking(bookingId);
  }

  @Override
  @Transactional(readOnly = true)
  public PricedPenalty priceCancellation(long bookingId, BigDecimal percent) {
    BigDecimal base = assessments.findPassengerFare(bookingId).orElse(ZERO);
    BigDecimal fee = PenaltyPolicy.fee(base, percent);
    PenaltySplit split = PenaltyPolicy.split(fee, policy.decimal(PolicyKey.PENALTY_VICTIM_PCT));
    return new PricedPenalty(
        base,
        percent == null ? ZERO : percent,
        split.fee(),
        split.victimShare(),
        split.platformShare());
  }
}
