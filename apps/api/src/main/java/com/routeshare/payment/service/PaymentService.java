package com.routeshare.payment.service;

import com.routeshare.payment.dto.request.CashCollectionRequest;
import com.routeshare.payment.dto.request.FareAdjustmentRequest;
import com.routeshare.payment.dto.request.PaymentIntentRequest;
import com.routeshare.payment.dto.request.PaymentLifecycleRequest;
import com.routeshare.payment.dto.response.ReceiptResponse;
import java.util.Map;

public interface PaymentService {
  Map<String, Object> createIntent(PaymentIntentRequest req);

  Map<String, Object> capture(long paymentIntentId, PaymentLifecycleRequest req);

  Map<String, Object> voidIntent(long paymentIntentId, PaymentLifecycleRequest req);

  Map<String, Object> refund(long paymentIntentId, PaymentLifecycleRequest req);

  Map<String, Object> recordCashCollected(long bookingId, CashCollectionRequest req);

  ReceiptResponse receipt(long bookingId);

  Map<String, Object> requestFareAdjustment(long bookingId, FareAdjustmentRequest req);

  Map<String, Object> driverEarningsSummary();

  java.util.List<Map<String, Object>> driverEarningsTransactions();

  java.util.List<Map<String, Object>> adminPayments();

  Map<String, Object> adminPaymentDetail(long paymentIntentId);

  java.util.List<Map<String, Object>> adminPaymentEvents(long paymentIntentId);

  java.util.List<Map<String, Object>> adminCashCollections();
}
