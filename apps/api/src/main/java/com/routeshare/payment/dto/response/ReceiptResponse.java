package com.routeshare.payment.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ReceiptResponse(
    Long bookingId,
    BigDecimal fareEstimate,
    BigDecimal paidAmount,
    BigDecimal refundedAmount,
    BigDecimal cashCollectedAmount,
    BigDecimal balanceDue,
    String currency,
    List<LineItem> lineItems) {
  public record LineItem(String type, BigDecimal amount, String currency) {}
}
