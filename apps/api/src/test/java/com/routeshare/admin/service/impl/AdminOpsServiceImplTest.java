package com.routeshare.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.admin.dto.ReportExportRequest;
import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.repository.BookingStatusHistoryRepository;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.trip.domain.TripStatus;
import com.routeshare.trip.entity.TripEntity;
import com.routeshare.trip.repository.TripRepository;
import com.routeshare.vehicle.repository.VehicleRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdminOpsServiceImplTest {
  private final TripRepository trips = mock(TripRepository.class);
  private final BookingRepository bookings = mock(BookingRepository.class);
  private final BookingStatusHistoryRepository bookingHistory =
      mock(BookingStatusHistoryRepository.class);
  private final DriverProfileRepository drivers = mock(DriverProfileRepository.class);
  private final VehicleRepository vehicles = mock(VehicleRepository.class);
  private final AdminAuditService audit = mock(AdminAuditService.class);
  private final AdminOpsServiceImpl service =
      new AdminOpsServiceImpl(trips, bookings, bookingHistory, drivers, vehicles, audit);

  @Test
  void requestExportReturnsQueuedJobAndAudits() {
    var res = service.requestExport(new ReportExportRequest("BOOKINGS"));

    assertThat(res.status()).isEqualTo("QUEUED");
    assertThat(res.reportType()).isEqualTo("BOOKINGS");
    assertThat(res.jobId()).isNotBlank();
    verify(audit).record(any(), any(), any(), any());
  }

  @Test
  void cancelTripRejectsAlreadyCompletedTrip() {
    var trip = mock(TripEntity.class);
    when(trip.getStatus()).thenReturn(TripStatus.COMPLETED);
    when(trips.findById(1L)).thenReturn(Optional.of(trip));

    assertThatThrownBy(() -> service.cancelTrip(1L, "x")).isInstanceOf(IllegalStateException.class);
  }
}
