package com.routeshare.admin.service;

import com.routeshare.admin.dto.AdminUserResponse;
import com.routeshare.admin.dto.UserStatusHistoryResponse;
import java.util.List;

public interface AdminUserService {
  List<AdminUserResponse> list(int limit);

  AdminUserResponse get(long appUserId);

  AdminUserResponse suspend(long appUserId, String reason);

  AdminUserResponse activate(long appUserId, String reason);

  List<UserStatusHistoryResponse> statusHistory(long appUserId);
}
