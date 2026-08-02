package com.routeshare.penalty.service;

import com.routeshare.penalty.dto.response.AppliedDuesResponse;
import com.routeshare.penalty.dto.response.DuesResponse;
import java.math.BigDecimal;

/**
 * Fees that could not be taken when they were assessed, carried to the next booking.
 *
 * <p>Dues never block. P25 and P09d both show them as a line added to a checkout, and refusing a
 * booking over an unpaid LKR 49 turns a small fee into a lost passenger. The only hard gate
 * anywhere near this is slice 05's prepay flag at two no-shows in a month, which is a different
 * rule about a different thing.
 */
public interface DuesService {

  /** P25. */
  DuesResponse dues(long appUserId);

  /** Records a due against a passenger who has no way to pay a fee right now. */
  void recordDue(long appUserId, long penaltyId, BigDecimal amount, String reason, Long bookingId);

  /** Attaches outstanding dues to a checkout (P09d). Does not settle them: capture does that. */
  AppliedDuesResponse applyOutstandingDues(long appUserId, long bookingId);

  /** The carrying booking captured, so the fees it carried are paid. */
  void settleDuesForBooking(long bookingId);

  /** The carrying booking fell through. The fees ride on to the next one. */
  void releaseDuesForBooking(long bookingId);

  /** Clears the due behind a penalty an admin has reversed. */
  void waiveDueForPenalty(long penaltyId);
}
