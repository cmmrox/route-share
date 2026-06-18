package com.routeshare.notification.dto;

/** Partial preference update; null fields are left unchanged. */
public record UpdatePreferencesRequest(
    Boolean pushEnabled,
    Boolean emailEnabled,
    Boolean bookingUpdates,
    Boolean tripUpdates,
    Boolean paymentUpdates,
    Boolean marketing) {}
