package com.routeshare.booking.service;

import com.routeshare.booking.dto.response.CounterpartyContactResponse;

/**
 * The counterparty's phone number, under the rules in plan §6.1.
 *
 * <p>Calls are direct dial (decision D5), so the app must be handed a real mobile number — and this
 * is the single place where a mistake becomes permanent. A number disclosed once cannot be
 * recalled; the person who has it keeps it after the ride, after the account is deleted, and after
 * any apology.
 *
 * <p>Five rules, all enforced here rather than by the caller:
 *
 * <ol>
 *   <li><b>Confirmed only.</b> Never on search, ride detail or a pending request — otherwise a
 *       driver could browse requests and collect numbers without ever accepting one.
 *   <li><b>Reciprocal.</b> Both sides get the other's number or neither does.
 *   <li><b>Scoped.</b> A passenger sees the driver of her own booking; a driver sees passengers
 *       holding a confirmed seat on the trip he is running. Nobody sees anyone else.
 *   <li><b>Revoked</b> 24 hours after drop-off, and immediately on any terminal state.
 *   <li><b>Audited</b> on every read, including repeats.
 * </ol>
 *
 * <p>Deliberately one method. If a relay is ever reinstated, it replaces this implementation
 * without a single caller changing — which is the whole reason the masking toggles could be cut
 * from the UI.
 */
public interface ContactDisclosureService {

  /**
   * @throws com.routeshare.common.errors.GateConflictException {@code CONTACT_NOT_AVAILABLE} when
   *     any rule above refuses — the caller is never told which, because a precise refusal is
   *     itself a probe
   * @throws org.springframework.security.access.AccessDeniedException when the caller is neither
   *     side of this booking
   */
  CounterpartyContactResponse counterpartyFor(long bookingId, long readerAppUserId);
}
