package com.routeshare.payment.dto.request;

import jakarta.validation.constraints.NotNull;

public record PaymentIntentRequest(@NotNull Long bookingId) {}
