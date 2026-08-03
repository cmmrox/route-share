package com.routeshare.location.service;

import com.routeshare.location.dto.response.RealtimeTokenResponse;

public interface RealtimeChannelService {
  RealtimeTokenResponse issueToken();

  long consumeToken(String token);

  void connect(long appUserId, String connectionId, String transport);

  void disconnect(String connectionId);

  boolean isConnected(long appUserId);

  int purgeExpired();
}
