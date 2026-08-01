package com.routeshare.admin.service.impl;

import com.routeshare.admin.dto.AdminUserResponse;
import com.routeshare.admin.dto.UserStatusHistoryResponse;
import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.admin.service.AdminUserService;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.entity.AppUserEntity;
import com.routeshare.identity.entity.AppUserStatusHistoryEntity;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.identity.repository.AppUserRepository;
import com.routeshare.identity.repository.AppUserStatusHistoryRepository;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin user management. Suspension is genuinely enforced: a SUSPENDED/DELETED user is rejected by
 * {@code AppUserRepository.upsertFromToken} on their next request. Every change is recorded to the
 * status history and the audit log.
 */
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {
  private static final String ACTIVE = "ACTIVE";
  private static final String SUSPENDED = "SUSPENDED";
  private static final int MAX_LIMIT = 200;

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final AppUserRepository users;
  private final AppUserStatusHistoryRepository statusHistory;
  private final AdminAuditService audit;

  @Override
  @Transactional(readOnly = true)
  public List<AdminUserResponse> list(int limit) {
    int capped = Math.min(limit <= 0 ? 50 : limit, MAX_LIMIT);
    return users.findAllByOrderByIdDesc(PageRequest.of(0, capped)).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public AdminUserResponse get(long appUserId) {
    return toResponse(require(appUserId));
  }

  @Override
  @Transactional
  public AdminUserResponse suspend(long appUserId, String reason) {
    return changeStatus(appUserId, SUSPENDED, reason);
  }

  @Override
  @Transactional
  public AdminUserResponse activate(long appUserId, String reason) {
    return changeStatus(appUserId, ACTIVE, reason);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserStatusHistoryResponse> statusHistory(long appUserId) {
    return statusHistory.findByAppUserIdOrderByIdDesc(appUserId).stream()
        .map(
            h ->
                new UserStatusHistoryResponse(
                    h.getId(),
                    h.getFromStatus(),
                    h.getToStatus(),
                    h.getReason(),
                    h.getChangedBy(),
                    h.getCreatedAt()))
        .toList();
  }

  @Override
  @Transactional
  public AdminUserResponse updateRoles(long appUserId, java.util.Set<String> roles) {
    var user = require(appUserId);
    identityFacade.setRealmRoles(
        user.getKeycloakSubject(), roles == null ? java.util.Set.of() : roles);
    audit.record(
        "USER_ROLES_UPDATED",
        "APP_USER",
        String.valueOf(appUserId),
        "{\"roles\":\"" + roles + "\"}");
    return toResponse(user);
  }

  private AdminUserResponse changeStatus(long appUserId, String toStatus, String reason) {
    var user = require(appUserId);
    String from = user.getLocalStatus();
    if (!toStatus.equals(from)) {
      user.setLocalStatus(toStatus);
      users.save(user);
      // Drop the cached identity projection so the new status is enforced on the next request.
      identityFacade.invalidateProjection(user.getKeycloakSubject());
      statusHistory.save(
          AppUserStatusHistoryEntity.of(
              appUserId,
              from,
              toStatus,
              reason,
              caseRef(appUserId, toStatus),
              currentAdminAppUserId()));
      audit.record(
          "USER_STATUS_CHANGED",
          "APP_USER",
          String.valueOf(appUserId),
          "{\"from\":\"" + from + "\",\"to\":\"" + toStatus + "\"}");
    }
    return toResponse(user);
  }

  /**
   * S13 shows the user a case reference to quote when they appeal, so a suspension needs one the
   * moment it is recorded. Reinstatement is not a case the user chases, so it gets none.
   */
  private String caseRef(long appUserId, String toStatus) {
    if (!SUSPENDED.equals(toStatus)) {
      return null;
    }
    return "SL-%d-%s"
        .formatted(
            appUserId, UUID.randomUUID().toString().substring(0, 5).toUpperCase(Locale.ROOT));
  }

  private AppUserEntity require(long appUserId) {
    return users
        .findById(appUserId)
        .orElseThrow(() -> new NoSuchElementException("User not found"));
  }

  private long currentAdminAppUserId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }

  private AdminUserResponse toResponse(AppUserEntity e) {
    return new AdminUserResponse(
        e.getId(),
        e.getPublicId() == null ? null : e.getPublicId().toString(),
        e.getEmail(),
        e.getPhone(),
        e.getDisplayName(),
        e.getLocalStatus());
  }
}
