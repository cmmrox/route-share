package com.routeshare.notification.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.notification.dto.*;
import com.routeshare.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("isAuthenticated()")
public class UnifiedNotificationController {
  private final NotificationService notifications;

  public UnifiedNotificationController(NotificationService notifications) {
    this.notifications = notifications;
  }

  @GetMapping("/api/v1/notifications")
  ApiResponse<List<NotificationResponse>> list(
      @RequestParam(defaultValue = "ALL") String filter,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "30") int size) {
    return ApiResponse.ok(notifications.listMine(filter, page, size));
  }

  @PostMapping("/api/v1/notifications/read-all")
  ApiResponse<Integer> readAll() {
    return ApiResponse.ok(notifications.markAllRead());
  }

  @GetMapping("/api/v1/notification-preferences")
  ApiResponse<NotificationPreferenceResponse> preferences() {
    return ApiResponse.ok(notifications.preferences());
  }

  @PutMapping("/api/v1/notification-preferences")
  ApiResponse<NotificationPreferenceResponse> preferences(
      @Valid @RequestBody UpdatePreferencesRequest request) {
    return ApiResponse.ok(notifications.savePreferences(request));
  }

  @GetMapping("/api/v1/badges")
  ApiResponse<BadgeSummaryResponse> badges() {
    return ApiResponse.ok(notifications.badges());
  }
}
