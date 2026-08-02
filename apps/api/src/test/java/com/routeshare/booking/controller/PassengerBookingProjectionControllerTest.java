package com.routeshare.booking.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.dto.response.PassengerBookingSummaryResponse;
import com.routeshare.booking.service.BookingService;
import com.routeshare.booking.service.EarlyDropOffService;
import com.routeshare.payment.service.PaymentService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PassengerBookingProjectionControllerTest {
  @Mock private BookingService bookings;
  @Mock private PaymentService payments;
  @Mock private EarlyDropOffService earlyDropOff;

  @Test
  void passengerBookingListDelegatesToProjectionService() {
    var controller =
        new PassengerBookingController(
            bookings,
            payments,
            earlyDropOff,
            org.mockito.Mockito.mock(com.routeshare.trip.facade.TripTimerFacade.class));
    var summary =
        new PassengerBookingSummaryResponse(
            1L,
            2L,
            3L,
            4L,
            "A",
            "B",
            Instant.parse("2026-06-02T01:00:00Z"),
            1,
            "CONFIRMED",
            "SCHEDULED",
            null,
            BigDecimal.TEN,
            null,
            Instant.parse("2026-06-01T01:00:00Z"));
    when(bookings.listPassengerBookings()).thenReturn(List.of(summary));

    var response = controller.list();

    assertThat(response.success()).isTrue();
    assertThat(response.data()).containsExactly(summary);
    verify(bookings).listPassengerBookings();
  }
}
