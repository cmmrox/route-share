package com.routeshare.payment.dto.response;

public record PaymentMethodResponse(
    long id,
    String brand,
    String last4,
    Integer expMonth,
    Integer expYear,
    boolean defaultMethod,
    String status) {}
