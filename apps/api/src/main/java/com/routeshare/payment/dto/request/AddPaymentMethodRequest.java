package com.routeshare.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Adds a stored card. {@code transientToken} is the short-lived token produced by Cybersource
 * Microform on the client — the PAN never reaches RouteShare.
 */
public record AddPaymentMethodRequest(@NotBlank String transientToken, boolean makeDefault) {}
