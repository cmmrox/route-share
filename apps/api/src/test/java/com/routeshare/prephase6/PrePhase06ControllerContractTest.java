package com.routeshare.prephase6;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.service.BookingService;
import com.routeshare.payment.controller.AdminPaymentController;
import com.routeshare.payment.controller.DriverEarningsController;
import com.routeshare.payment.dto.request.FareAdjustmentRequest;
import com.routeshare.payment.service.PaymentService;
import com.routeshare.routing.controller.DriverRouteController;
import com.routeshare.routing.service.RouteService;
import com.routeshare.trip.controller.DriverTripController;
import com.routeshare.trip.dto.request.PreTripChecklistRequest;
import com.routeshare.trip.service.TripService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PrePhase06ControllerContractTest {
  @Test
  void driverRouteShareLinkDelegatesToRouteService() {
    RouteService routes = org.mockito.Mockito.mock(RouteService.class);
    var controller = new DriverRouteController(routes);
    when(routes.createShareLink(44L))
        .thenReturn(Map.of("routeId", 44L, "shareUrl", "https://routeshare.local/routes/44"));

    var response = controller.shareLink(44L);

    assertThat(response.data()).containsEntry("routeId", 44L);
    verify(routes).createShareLink(44L);
  }

  @Test
  void driverTripOpsDelegateToTripService() {
    TripService trips = org.mockito.Mockito.mock(TripService.class);
    BookingService bookings = org.mockito.Mockito.mock(BookingService.class);
    var startWindows =
        org.mockito.Mockito.mock(com.routeshare.trip.service.TripStartWindowService.class);
    var controller = new DriverTripController(trips, bookings, startWindows);
    var checklist = new PreTripChecklistRequest(true, true, true, "ready");
    when(trips.recordPreTripChecklist(9L, checklist))
        .thenReturn(Map.of("tripId", 9L, "status", "CHECKLIST_RECORDED"));
    when(trips.markArrivedPickup(9L)).thenReturn(Map.of("tripId", 9L, "status", "ARRIVED_PICKUP"));

    assertThat(controller.preTripChecklist(9L, checklist).data())
        .containsEntry("status", "CHECKLIST_RECORDED");
    assertThat(controller.arrivedPickup(9L).data()).containsEntry("status", "ARRIVED_PICKUP");
    verify(trips).recordPreTripChecklist(9L, checklist);
    verify(trips).markArrivedPickup(9L);
  }

  @Test
  void driverFareAdjustmentAndEarningsDelegateToPaymentService() {
    PaymentService payments = org.mockito.Mockito.mock(PaymentService.class);
    var earnings = new DriverEarningsController(payments);
    var request = new FareAdjustmentRequest(new BigDecimal("125.00"), "detour");
    when(payments.requestFareAdjustment(88L, request))
        .thenReturn(Map.of("bookingId", 88L, "status", "FARE_ADJUSTMENT_REQUESTED"));
    when(payments.driverEarningsSummary())
        .thenReturn(Map.of("currency", "LKR", "totalEarnings", BigDecimal.TEN));
    when(payments.driverEarningsTransactions())
        .thenReturn(List.of(Map.of("type", "PAYMENT_CAPTURED")));

    assertThat(earnings.fareAdjustment(88L, request).data())
        .containsEntry("status", "FARE_ADJUSTMENT_REQUESTED");
    assertThat(earnings.summary().data()).containsEntry("currency", "LKR");
    assertThat(earnings.transactions().data()).hasSize(1);
  }

  @Test
  void adminPaymentProjectionsDelegateToPaymentService() {
    PaymentService payments = org.mockito.Mockito.mock(PaymentService.class);
    var controller = new AdminPaymentController(payments);
    when(payments.adminPayments()).thenReturn(List.of(Map.of("paymentIntentId", 1L)));
    when(payments.adminPaymentDetail(1L)).thenReturn(Map.of("paymentIntentId", 1L));
    when(payments.adminPaymentEvents(1L))
        .thenReturn(List.of(Map.of("entryType", "PAYMENT_CAPTURED")));
    when(payments.adminCashCollections())
        .thenReturn(List.of(Map.of("entryType", "CASH_COLLECTED")));

    assertThat(controller.list().data()).hasSize(1);
    assertThat(controller.detail(1L).data()).containsEntry("paymentIntentId", 1L);
    assertThat(controller.events(1L).data()).hasSize(1);
    assertThat(controller.cashCollections().data()).hasSize(1);
  }
}
