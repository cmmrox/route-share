package com.routeshare.admin.service;

import com.routeshare.admin.dto.AdminSosResponse;
import java.util.List;

public interface AdminSafetyService {
  List<AdminSosResponse> list(String status, int limit);

  AdminSosResponse get(long sosEventId);

  AdminSosResponse resolve(long sosEventId, String resolutionNote);
}
