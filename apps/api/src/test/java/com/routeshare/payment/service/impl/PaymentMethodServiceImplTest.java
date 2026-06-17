package com.routeshare.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.payment.dto.request.AddPaymentMethodRequest;
import com.routeshare.payment.entity.PaymentMethodEntity;
import com.routeshare.payment.gateway.PaymentGatewayPort;
import com.routeshare.payment.gateway.PaymentGatewayPort.TokenizationResult;
import com.routeshare.payment.repository.PaymentMethodRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class PaymentMethodServiceImplTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final PaymentMethodRepository repository = mock(PaymentMethodRepository.class);
  private final PaymentGatewayPort gateway = mock(PaymentGatewayPort.class);
  private final PaymentMethodServiceImpl service =
      new PaymentMethodServiceImpl(current, identityFacade, repository, gateway);

  @BeforeEach
  void setUp() {
    var user = new CurrentUser("sub", "p@test", null, "Passenger", Set.of("PASSENGER"));
    var appUser = new AppUser(5L, UUID.randomUUID(), "sub", "p@test", null, "Passenger", "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
  }

  @Test
  void addTokenizesAndMakesFirstCardDefault() {
    when(gateway.tokenizeCard("tok_jwt"))
        .thenReturn(new TokenizationResult("instr_1", "VISA", "4242", 12, 2030));
    when(gateway.cardPaymentsEnabled()).thenReturn(true);
    when(repository.findByAppUserIdAndStatusOrderByIdDesc(5L, "ACTIVE")).thenReturn(List.of());
    when(repository.save(any(PaymentMethodEntity.class)))
        .thenAnswer(
            inv -> {
              PaymentMethodEntity e = inv.getArgument(0);
              e.setId(1L);
              return e;
            });

    var res = service.add(new AddPaymentMethodRequest("tok_jwt", false));

    assertThat(res.brand()).isEqualTo("VISA");
    assertThat(res.last4()).isEqualTo("4242");
    assertThat(res.defaultMethod()).isTrue();
    verify(repository).clearDefaults(5L); // first card becomes default
    verify(gateway).tokenizeCard("tok_jwt");
  }

  @Test
  void setDefaultClearsOthersAndSetsThisOne() {
    var method =
        PaymentMethodEntity.active(5L, "CYBERSOURCE", "instr_1", "VISA", "4242", 12, 2030, false);
    method.setId(9L);
    when(repository.findByIdAndAppUserId(9L, 5L)).thenReturn(Optional.of(method));

    var res = service.setDefault(9L);

    verify(repository).clearDefaults(5L);
    assertThat(method.isDefaultMethod()).isTrue();
    assertThat(res.defaultMethod()).isTrue();
  }

  @Test
  void deleteMarksRemoved() {
    var method =
        PaymentMethodEntity.active(5L, "CYBERSOURCE", "instr_1", "VISA", "4242", 12, 2030, true);
    when(repository.findByIdAndAppUserId(9L, 5L)).thenReturn(Optional.of(method));

    service.delete(9L);

    assertThat(method.getStatus()).isEqualTo(PaymentMethodEntity.STATUS_REMOVED);
    assertThat(method.isDefaultMethod()).isFalse();
  }

  @Test
  void operationsOnOthersCardAreDenied() {
    when(repository.findByIdAndAppUserId(9L, 5L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.setDefault(9L)).isInstanceOf(AccessDeniedException.class);
  }
}
