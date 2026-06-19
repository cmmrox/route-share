package com.routeshare.admin.service.impl;

import com.routeshare.admin.dto.AdminDashboardResponse;
import com.routeshare.admin.service.AdminDashboardService;
import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.finance.entity.PayoutBatchEntity;
import com.routeshare.finance.repository.PayoutBatchRepository;
import com.routeshare.identity.repository.AppUserRepository;
import com.routeshare.payment.repository.PaymentIntentRepository;
import com.routeshare.safety.entity.SosEventEntity;
import com.routeshare.safety.repository.SosEventRepository;
import com.routeshare.support.entity.SupportTicketEntity;
import com.routeshare.support.repository.SupportTicketRepository;
import com.routeshare.trip.repository.TripRepository;
import com.routeshare.vehicle.repository.VehicleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Live operational counts for the admin dashboard, computed from the real domain tables. */
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {
  private final AppUserRepository users;
  private final DriverProfileRepository drivers;
  private final VehicleRepository vehicles;
  private final BookingRepository bookings;
  private final TripRepository trips;
  private final PaymentIntentRepository paymentIntents;
  private final SosEventRepository sosEvents;
  private final SupportTicketRepository supportTickets;
  private final PayoutBatchRepository payoutBatches;

  @Override
  @Transactional(readOnly = true)
  public AdminDashboardResponse summary() {
    return new AdminDashboardResponse(
        users.count(),
        drivers.count(),
        vehicles.count(),
        bookings.count(),
        trips.count(),
        paymentIntents.count(),
        sosEvents.countByStatus(SosEventEntity.RAISED),
        supportTickets.countByStatusIn(
            List.of(SupportTicketEntity.OPEN, SupportTicketEntity.PENDING)),
        payoutBatches.countByStatus(PayoutBatchEntity.OPEN));
  }
}
