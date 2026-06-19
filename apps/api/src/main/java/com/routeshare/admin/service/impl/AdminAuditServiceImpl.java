package com.routeshare.admin.service.impl;

import com.routeshare.admin.dto.AuditActionResponse;
import com.routeshare.admin.entity.AuditActionEntity;
import com.routeshare.admin.repository.AuditActionRepository;
import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import java.util.List;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuditServiceImpl implements AdminAuditService {
  private static final int MAX_LIMIT = 500;

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final AuditActionRepository auditActions;

  @Override
  @Transactional
  public void record(String action, String targetType, String targetId, String detailJson) {
    var user = current.requireCurrentUser();
    var app = identityFacade.upsertFromToken(user);
    String role =
        user.roles() == null || user.roles().isEmpty()
            ? null
            : String.join(",", new TreeSet<>(user.roles()));
    auditActions.save(
        AuditActionEntity.of(app.appUserId(), role, action, targetType, targetId, detailJson));
  }

  @Override
  @Transactional(readOnly = true)
  public List<AuditActionResponse> recent(int limit) {
    int capped = Math.min(limit <= 0 ? 100 : limit, MAX_LIMIT);
    return auditActions.findAllByOrderByIdDesc(PageRequest.of(0, capped)).stream()
        .map(this::toResponse)
        .toList();
  }

  private AuditActionResponse toResponse(AuditActionEntity e) {
    return new AuditActionResponse(
        e.getId(),
        e.getActorAppUserId(),
        e.getActorRole(),
        e.getAction(),
        e.getTargetType(),
        e.getTargetId(),
        e.getDetailJson(),
        e.getCreatedAt());
  }
}
