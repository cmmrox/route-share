package com.routeshare.booking.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.dto.request.BookingRequest;
import com.routeshare.booking.service.BookingService;
import com.routeshare.booking.service.EarlyDropOffService;
import com.routeshare.payment.service.PaymentService;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PassengerBookingAliasControllerTest {
  @Mock private BookingService bookings;
  @Mock private PaymentService payments;
  @Mock private EarlyDropOffService earlyDropOff;

  @Test
  void passengerBookingCreateDelegatesWithIdempotencyKey() {
    var controller =
        new PassengerBookingController(
            bookings,
            payments,
            earlyDropOff,
            org.mockito.Mockito.mock(com.routeshare.trip.facade.TripTimerFacade.class));
    var request =
        new BookingRequest(20L, 1, 6.9271, 79.8612, 6.9000, 79.9000, 0.10, 0.80, null, null);
    when(bookings.book(request, "idem-1"))
        .thenReturn(
            Map.of("bookingId", 30L, "status", "CONFIRMED", "fareEstimate", BigDecimal.TEN));

    var response = controller.create("idem-1", request);

    assertThat(response.success()).isTrue();
    assertThat(response.data())
        .containsEntry("bookingId", 30L)
        .containsEntry("status", "CONFIRMED");
    verify(bookings).book(request, "idem-1");
  }

  @Test
  void passengerCancelDelegatesToBookingStatusTransition() {
    var controller =
        new PassengerBookingController(
            bookings,
            payments,
            earlyDropOff,
            org.mockito.Mockito.mock(com.routeshare.trip.facade.TripTimerFacade.class));
    when(bookings.transition(
            30L,
            new com.routeshare.booking.dto.request.BookingStatusTransitionRequest(
                "CANCELLED", "Passenger cancelled")))
        .thenReturn(Map.of("bookingId", 30L, "status", "CANCELLED"));

    var response =
        controller.cancel(
            30L, new PassengerBookingController.CancelBookingRequest("Passenger cancelled"));

    assertThat(response.success()).isTrue();
    assertThat(response.data()).containsEntry("status", "CANCELLED");
    verify(bookings)
        .transition(
            org.mockito.Mockito.eq(30L),
            argThat(
                req ->
                    req.status().equals("CANCELLED")
                        && req.reason().equals("Passenger cancelled")));
  }
}
