package com.routeshare.admin.service;

import com.routeshare.admin.dto.AuditActionResponse;
import java.util.List;

public interface AdminAuditService {
  /**
   * Records an admin action performed by the current authenticated admin. Joins the caller's tx.
   */
  void record(String action, String targetType, String targetId, String detailJson);

  List<AuditActionResponse> recent(int limit);
}
