package com.routeshare.payment.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Creates a payment intent for a booking. {@code paymentMethodId} selects a stored card to
 * authorize against; when null the intent is treated as cash (no external authorization).
 */
public record PaymentIntentRequest(@NotNull Long bookingId, Long paymentMethodId) {}
