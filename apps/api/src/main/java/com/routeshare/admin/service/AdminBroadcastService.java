package com.routeshare.admin.service;

import com.routeshare.admin.dto.BroadcastRequest;
import java.util.Map;

public interface AdminBroadcastService {
  /** Sends a broadcast notification to all users with an enabled push registration. */
  Map<String, Object> broadcast(BroadcastRequest req);
}
