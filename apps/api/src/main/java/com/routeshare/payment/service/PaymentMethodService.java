package com.routeshare.payment.service;

import com.routeshare.payment.dto.request.AddPaymentMethodRequest;
import com.routeshare.payment.dto.response.PaymentMethodResponse;
import java.util.List;

public interface PaymentMethodService {
  List<PaymentMethodResponse> listMine();

  PaymentMethodResponse add(AddPaymentMethodRequest req);

  void delete(long paymentMethodId);

  PaymentMethodResponse setDefault(long paymentMethodId);
}
