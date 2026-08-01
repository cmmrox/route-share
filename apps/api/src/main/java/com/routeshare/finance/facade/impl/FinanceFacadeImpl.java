package com.routeshare.finance.facade.impl;

import com.routeshare.finance.entity.FarePolicyEntity;
import com.routeshare.finance.facade.FinanceFacade;
import com.routeshare.finance.repository.FarePolicyRepository;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FinanceFacadeImpl implements FinanceFacade {
  private final FarePolicyRepository farePolicies;

  @Override
  @Transactional(readOnly = true)
  public Optional<BigDecimal> activeMinFare() {
    return farePolicies.findFirstByActiveTrueOrderByIdDesc().map(FarePolicyEntity::getMinFare);
  }
}
