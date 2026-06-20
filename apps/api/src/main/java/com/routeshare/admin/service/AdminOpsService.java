package com.routeshare.admin.service;

import com.routeshare.admin.dto.AdminBookingResponse;
import com.routeshare.admin.dto.AdminBookingStatusHistoryResponse;
import com.routeshare.admin.dto.AdminDriverApplicationResponse;
import com.routeshare.admin.dto.AdminLocationSampleResponse;
import com.routeshare.admin.dto.AdminTripResponse;
import com.routeshare.admin.dto.AdminVehicleResponse;
import com.routeshare.admin.dto.ReportExportRequest;
import com.routeshare.admin.dto.ReportExportResponse;
import java.util.List;

/** Admin operations: trip/booking read projections + cancel, verification lists, report export. */
public interface AdminOpsService {
  List<AdminTripResponse> listTrips(int limit);

  AdminTripResponse getTrip(long tripId);

  AdminTripResponse cancelTrip(long tripId, String reason);

  List<AdminLocationSampleResponse> locationTrail(long tripId);

  List<AdminBookingResponse> listBookings(int limit);

  AdminBookingResponse getBooking(long bookingId);

  List<AdminBookingStatusHistoryResponse> bookingStatusHistory(long bookingId);

  List<AdminDriverApplicationResponse> listDriverApplications(int limit);

  AdminDriverApplicationResponse getDriverApplication(long driverProfileId);

  List<AdminVehicleResponse> listVehicles(int limit);

  AdminVehicleResponse getVehicle(long vehicleId);

  ReportExportResponse requestExport(ReportExportRequest req);
}
