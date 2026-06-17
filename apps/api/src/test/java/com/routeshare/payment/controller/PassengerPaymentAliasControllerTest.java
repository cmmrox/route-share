package com.routeshare.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.payment.dto.request.PaymentIntentRequest;
import com.routeshare.payment.service.PaymentService;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PassengerPaymentAliasControllerTest {
  @Mock private PaymentService payments;

  @Test
  void passengerPaymentIntentDelegatesToPaymentService() {
    var controller = new PassengerPaymentController(payments);
    var request = new PaymentIntentRequest(30L, null);
    when(payments.createIntent(request))
        .thenReturn(
            Map.of("status", "REQUIRES_CAPTURE", "amount", BigDecimal.TEN, "currency", "LKR"));

    var response = controller.createIntent(request);

    assertThat(response.success()).isTrue();
    assertThat(response.data()).containsEntry("status", "REQUIRES_CAPTURE");
    verify(payments).createIntent(request);
  }
}
