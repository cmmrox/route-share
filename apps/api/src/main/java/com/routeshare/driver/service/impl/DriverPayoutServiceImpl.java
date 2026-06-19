package com.routeshare.driver.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.dto.request.PayoutProfileRequest;
import com.routeshare.driver.dto.response.PayoutProfileResponse;
import com.routeshare.driver.entity.DriverPayoutProfileEntity;
import com.routeshare.driver.repository.DriverPayoutProfileRepository;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.driver.service.DriverPayoutService;
import com.routeshare.identity.facade.IdentityFacade;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DriverPayoutServiceImpl implements DriverPayoutService {
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final DriverProfileRepository drivers;
  private final DriverPayoutProfileRepository payoutProfiles;

  @Override
  @Transactional(readOnly = true)
  public PayoutProfileResponse getMine() {
    long driverProfileId = currentDriverProfileId();
    return payoutProfiles
        .findById(driverProfileId)
        .map(this::toResponse)
        .orElseGet(
            () ->
                new PayoutProfileResponse(
                    false, null, null, null, null, null, null, null, "NOT_CONFIGURED", null));
  }

  @Override
  @Transactional
  public PayoutProfileResponse saveMine(PayoutProfileRequest req) {
    validate(req);
    long driverProfileId = currentDriverProfileId();
    var profile =
        payoutProfiles
            .findById(driverProfileId)
            .orElseGet(() -> DriverPayoutProfileEntity.blank(driverProfileId));
    profile.setMethod(req.method());
    profile.setBankName(req.bankName());
    profile.setBranch(req.branch());
    profile.setAccountName(req.accountName());
    profile.setAccountNumber(req.accountNumber());
    profile.setWalletProvider(req.walletProvider());
    profile.setWalletNumber(req.walletNumber());
    // Any change requires re-verification before payouts are released.
    profile.setStatus(DriverPayoutProfileEntity.PENDING);
    profile.setUpdatedAt(Instant.now());
    return toResponse(payoutProfiles.save(profile));
  }

  private void validate(PayoutProfileRequest req) {
    if ("BANK_TRANSFER".equals(req.method())) {
      if (isBlank(req.bankName()) || isBlank(req.accountName()) || isBlank(req.accountNumber())) {
        throw new IllegalArgumentException(
            "Bank transfer requires bankName, accountName and accountNumber");
      }
    } else if ("MOBILE_WALLET".equals(req.method())) {
      if (isBlank(req.walletProvider()) || isBlank(req.walletNumber())) {
        throw new IllegalArgumentException(
            "Mobile wallet requires walletProvider and walletNumber");
      }
    }
  }

  private long currentDriverProfileId() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return drivers
        .findIdByAppUserId(app.appUserId())
        .orElseThrow(() -> new AccessDeniedException("Driver profile is required"));
  }

  private PayoutProfileResponse toResponse(DriverPayoutProfileEntity e) {
    return new PayoutProfileResponse(
        true,
        e.getMethod(),
        e.getBankName(),
        e.getBranch(),
        e.getAccountName(),
        mask(e.getAccountNumber()),
        e.getWalletProvider(),
        mask(e.getWalletNumber()),
        e.getStatus(),
        e.getUpdatedAt());
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static String mask(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String digits = value.trim();
    if (digits.length() <= 4) {
      return "••••";
    }
    return "••••" + digits.substring(digits.length() - 4);
  }
}
