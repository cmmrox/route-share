package com.routeshare.admin.service.impl;

import com.routeshare.admin.dto.BroadcastRequest;
import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.admin.service.AdminBroadcastService;
import com.routeshare.identity.repository.AppUserRepository;
import com.routeshare.notification.facade.NotificationFacade;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminBroadcastServiceImpl implements AdminBroadcastService {
  /** Safety cap on a single broadcast fan-out. */
  private static final int MAX_RECIPIENTS = 10_000;

  private final AppUserRepository appUsers;
  private final NotificationFacade notifications;
  private final AdminAuditService audit;

  @Override
  @Transactional
  public Map<String, Object> broadcast(BroadcastRequest req) {
    var recipients = appUsers.findActiveAppUserIds(PageRequest.of(0, MAX_RECIPIENTS));
    for (Long appUserId : recipients) {
      notifications.notifyUser(appUserId, "BROADCAST", req.title(), req.body(), null);
    }
    audit.record(
        "NOTIFICATION_BROADCAST", "BROADCAST", null, "{\"recipients\":" + recipients.size() + "}");
    return Map.of("recipients", recipients.size(), "title", req.title());
  }
}
