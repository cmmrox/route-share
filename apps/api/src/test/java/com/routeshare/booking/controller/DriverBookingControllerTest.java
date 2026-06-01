package com.routeshare.booking.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.service.BookingService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DriverBookingControllerTest {
  @Mock private BookingService bookings;

  @Test
  void approveDelegatesToDriverBookingApproval() {
    var controller = new DriverBookingController(bookings);
    when(bookings.approveByDriver(50L)).thenReturn(Map.of("bookingId", 50L, "status", "CONFIRMED"));

    var response = controller.approve(50L);

    assertThat(response.success()).isTrue();
    assertThat(response.data()).containsEntry("status", "CONFIRMED");
    verify(bookings).approveByDriver(50L);
  }

  @Test
  void declineDelegatesToDriverBookingDecline() {
    var controller = new DriverBookingController(bookings);
    when(bookings.declineByDriver(50L, "Full vehicle"))
        .thenReturn(Map.of("bookingId", 50L, "status", "REJECTED"));

    var response =
        controller.decline(50L, new DriverBookingController.DeclineBookingRequest("Full vehicle"));

    assertThat(response.success()).isTrue();
    assertThat(response.data()).containsEntry("status", "REJECTED");
    verify(bookings).declineByDriver(50L, "Full vehicle");
  }
}
