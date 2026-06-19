package com.routeshare.admin.dto;

public record AdminDashboardResponse(
    long totalUsers,
    long totalDrivers,
    long totalVehicles,
    long totalBookings,
    long totalTrips,
    long totalPaymentIntents,
    long openSosEvents,
    long openSupportTickets,
    long openPayoutBatches) {}
