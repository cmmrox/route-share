package com.routeshare.payment.service;

import com.routeshare.payment.dto.request.PaymentIntentRequest;
import java.util.Map;

public interface PaymentService {
  Map<String, Object> createIntent(PaymentIntentRequest req);
}
