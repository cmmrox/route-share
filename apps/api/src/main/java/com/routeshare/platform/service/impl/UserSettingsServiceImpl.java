package com.routeshare.platform.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.platform.dto.request.UserSettingsRequest;
import com.routeshare.platform.dto.response.*;
import com.routeshare.platform.entity.*;
import com.routeshare.platform.repository.*;
import com.routeshare.platform.service.UserSettingsService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements UserSettingsService {
  private static final int RECEIPT_RETENTION_YEARS = 7;
  private static final List<String> OPEN = List.of("QUEUED", "IN_PROGRESS");

  private final CurrentUserProvider current;
  private final IdentityFacade identity;
  private final UserSettingRepository settings;
  private final AccountRequestRepository accountRequests;

  @Override
  @Transactional
  public UserSettingsResponse mine() {
    return forAppUser(currentAppUserId());
  }

  @Override
  @Transactional
  public UserSettingsResponse forAppUser(long appUserId) {
    return toResponse(
        settings
            .findById(appUserId)
            .orElseGet(() -> settings.save(UserSettingEntity.defaultsFor(appUserId))));
  }

  @Override
  @Transactional
  public UserSettingsResponse update(UserSettingsRequest request) {
    long appUserId = currentAppUserId();
    var entity =
        settings.findById(appUserId).orElseGet(() -> UserSettingEntity.defaultsFor(appUserId));
    entity.setTheme(request.theme());
    entity.setLanguage(request.language());
    entity.setShareLiveLocation(request.shareLiveLocation());
    entity.setShowRatingPublicly(request.showRatingPublicly());
    entity.setReceiptsByEmail(request.receiptsByEmail());
    entity.setUpdatedAt(Instant.now());
    return toResponse(settings.save(entity));
  }

  @Override
  @Transactional
  public AccountRequestResponse requestDataExport() {
    return request("DATA_EXPORT", "A secure export will be prepared for the account owner.");
  }

  @Override
  @Transactional
  public AccountRequestResponse requestDeletion() {
    return request(
        "DELETION", "Deletion excludes financial receipts that must be retained for 7 years.");
  }

  @Override
  @Transactional(readOnly = true)
  public List<AccountRequestResponse> listAccountRequests() {
    return accountRequests.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
  }

  private AccountRequestResponse request(String kind, String note) {
    long appUserId = currentAppUserId();
    var entity =
        accountRequests
            .findFirstByAppUserIdAndKindAndStatusInOrderByIdDesc(appUserId, kind, OPEN)
            .orElseGet(
                () -> accountRequests.save(AccountRequestEntity.queued(appUserId, kind, note)));
    return toResponse(entity);
  }

  private long currentAppUserId() {
    return identity.upsertFromToken(current.requireCurrentUser()).appUserId();
  }

  private UserSettingsResponse toResponse(UserSettingEntity entity) {
    return new UserSettingsResponse(
        entity.getTheme(),
        entity.getLanguage(),
        entity.isShareLiveLocation(),
        entity.isShowRatingPublicly(),
        entity.isReceiptsByEmail(),
        entity.getUpdatedAt());
  }

  private AccountRequestResponse toResponse(AccountRequestEntity entity) {
    return new AccountRequestResponse(
        entity.getId(),
        entity.getAppUserId(),
        entity.getKind(),
        entity.getStatus(),
        entity.getRequestedAt(),
        RECEIPT_RETENTION_YEARS,
        entity.getNote());
  }
}
