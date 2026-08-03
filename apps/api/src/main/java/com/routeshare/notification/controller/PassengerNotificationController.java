package com.routeshare.notification.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.notification.dto.NotificationPreferenceResponse;
import com.routeshare.notification.dto.NotificationResponse;
import com.routeshare.notification.dto.RegisterPushRequest;
import com.routeshare.notification.dto.UpdatePreferencesRequest;
import com.routeshare.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('PASSENGER')")
public class PassengerNotificationController {
  private final NotificationService service;

  public PassengerNotificationController(NotificationService service) {
    this.service = service;
  }

  @Deprecated
  @GetMapping("/api/v1/passenger/notifications")
  ApiResponse<List<NotificationResponse>> list(
      @RequestParam(name = "limit", defaultValue = "30") int limit) {
    return ApiResponse.ok(service.listMine(limit));
  }

  @PostMapping("/api/v1/passenger/notifications/{notificationId}/read")
  ApiResponse<NotificationResponse> read(@PathVariable long notificationId) {
    return ApiResponse.ok(service.markRead(notificationId));
  }

  @Deprecated
  @GetMapping("/api/v1/passenger/notification-preferences")
  ApiResponse<NotificationPreferenceResponse> preferences() {
    return ApiResponse.ok(service.preferences());
  }

  @Deprecated
  @PutMapping("/api/v1/passenger/notification-preferences")
  ApiResponse<NotificationPreferenceResponse> updatePreferences(
      @RequestBody UpdatePreferencesRequest req) {
    return ApiResponse.ok(service.savePreferences(req));
  }

  @PostMapping("/api/v1/passenger/push-registrations")
  ApiResponse<Void> registerPush(@Valid @RequestBody RegisterPushRequest req) {
    service.registerPush(req);
    return ApiResponse.ok(null);
  }
}
