package com.routeshare.booking.controller;

import com.routeshare.booking.dto.response.CounterpartyContactResponse;
import com.routeshare.booking.service.ContactDisclosureService;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.identity.facade.IdentityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The counterparty's number, for a direct dial (plan §6.1).
 *
 * <p>Both paths reach the same service method deliberately: reciprocity is not something two
 * endpoints can be trusted to implement the same way, and a relay swapped in later must replace one
 * implementation rather than two.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingContactController {

  private final ContactDisclosureService contacts;
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;

  @GetMapping("/passenger/bookings/{bookingId}/contact")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<CounterpartyContactResponse> passengerContact(@PathVariable long bookingId) {
    return ApiResponse.ok(contacts.counterpartyFor(bookingId, appUserId()));
  }

  @GetMapping("/driver/bookings/{bookingId}/contact")
  @PreAuthorize("hasAnyRole('DRIVER','ADMIN','SUPER_ADMIN')")
  ApiResponse<CounterpartyContactResponse> driverContact(@PathVariable long bookingId) {
    return ApiResponse.ok(contacts.counterpartyFor(bookingId, appUserId()));
  }

  private long appUserId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }
}
