package com.routeshare.notification.dto;

public record NotificationPreferenceResponse(
    boolean pushEnabled,
    boolean emailEnabled,
    boolean bookingUpdates,
    boolean tripUpdates,
    boolean paymentUpdates,
    boolean marketing) {}
