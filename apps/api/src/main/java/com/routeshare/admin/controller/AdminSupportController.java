package com.routeshare.admin.controller;

import com.routeshare.admin.dto.AdminTicketUpdateRequest;
import com.routeshare.admin.service.AdminSupportService;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.support.dto.SupportMessageResponse;
import com.routeshare.support.dto.SupportTicketResponse;
import com.routeshare.support.dto.TicketMessageRequest;
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
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','SUPPORT_AGENT','OPS_ADMIN')")
public class AdminSupportController {
  private final AdminSupportService service;

  public AdminSupportController(AdminSupportService service) {
    this.service = service;
  }

  @GetMapping("/api/v1/admin/support/tickets")
  ApiResponse<List<SupportTicketResponse>> list(
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ApiResponse.ok(service.list(status, limit));
  }

  @GetMapping("/api/v1/admin/support/tickets/{ticketId}")
  ApiResponse<SupportTicketResponse> get(@PathVariable long ticketId) {
    return ApiResponse.ok(service.get(ticketId));
  }

  @PutMapping("/api/v1/admin/support/tickets/{ticketId}")
  ApiResponse<SupportTicketResponse> updateStatus(
      @PathVariable long ticketId, @Valid @RequestBody AdminTicketUpdateRequest req) {
    return ApiResponse.ok(service.updateStatus(ticketId, req.status()));
  }

  @PostMapping("/api/v1/admin/support/tickets/{ticketId}/messages")
  ApiResponse<SupportMessageResponse> reply(
      @PathVariable long ticketId, @Valid @RequestBody TicketMessageRequest req) {
    return ApiResponse.ok(service.reply(ticketId, req));
  }
}
