package com.routeshare.payment.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.payment.dto.request.AddPaymentMethodRequest;
import com.routeshare.payment.dto.response.PaymentMethodResponse;
import com.routeshare.payment.entity.PaymentMethodEntity;
import com.routeshare.payment.gateway.PaymentGatewayPort;
import com.routeshare.payment.repository.PaymentMethodRepository;
import com.routeshare.payment.service.PaymentMethodService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl implements PaymentMethodService {
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final PaymentMethodRepository repository;
  private final PaymentGatewayPort gateway;

  @Override
  @Transactional(readOnly = true)
  public List<PaymentMethodResponse> listMine() {
    return repository
        .findByAppUserIdAndStatusOrderByIdDesc(
            currentAppUserId(), PaymentMethodEntity.STATUS_ACTIVE)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public PaymentMethodResponse add(AddPaymentMethodRequest req) {
    long appUserId = currentAppUserId();
    var tokenized = gateway.tokenizeCard(req.transientToken());
    boolean makeDefault =
        req.makeDefault()
            || repository
                .findByAppUserIdAndStatusOrderByIdDesc(appUserId, PaymentMethodEntity.STATUS_ACTIVE)
                .isEmpty();
    if (makeDefault) {
      repository.clearDefaults(appUserId);
    }
    var saved =
        repository.save(
            PaymentMethodEntity.active(
                appUserId,
                gateway.cardPaymentsEnabled() ? "CYBERSOURCE" : "TEST",
                tokenized.token(),
                tokenized.brand(),
                tokenized.last4(),
                tokenized.expMonth(),
                tokenized.expYear(),
                makeDefault));
    return toResponse(saved);
  }

  @Override
  @Transactional
  public void delete(long paymentMethodId) {
    var method = requireOwned(paymentMethodId);
    method.setStatus(PaymentMethodEntity.STATUS_REMOVED);
    method.setDefaultMethod(false);
  }

  @Override
  @Transactional
  public PaymentMethodResponse setDefault(long paymentMethodId) {
    var method = requireOwned(paymentMethodId);
    repository.clearDefaults(method.getAppUserId());
    method.setDefaultMethod(true);
    return toResponse(method);
  }

  private PaymentMethodEntity requireOwned(long paymentMethodId) {
    return repository
        .findByIdAndAppUserId(paymentMethodId, currentAppUserId())
        .orElseThrow(
            () -> new AccessDeniedException("Payment method does not belong to current user"));
  }

  private long currentAppUserId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }

  private PaymentMethodResponse toResponse(PaymentMethodEntity e) {
    return new PaymentMethodResponse(
        e.getId(),
        e.getBrand(),
        e.getLast4(),
        e.getExpMonth(),
        e.getExpYear(),
        e.isDefaultMethod(),
        e.getStatus());
  }
}
