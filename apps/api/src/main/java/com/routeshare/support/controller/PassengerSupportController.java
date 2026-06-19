package com.routeshare.support.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.support.dto.CreateTicketRequest;
import com.routeshare.support.dto.SupportMessageResponse;
import com.routeshare.support.dto.SupportTicketResponse;
import com.routeshare.support.dto.TicketMessageRequest;
import com.routeshare.support.service.SupportService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/passenger/support/tickets")
@PreAuthorize("hasRole('PASSENGER')")
public class PassengerSupportController {
  private final SupportService service;

  public PassengerSupportController(SupportService service) {
    this.service = service;
  }

  @PostMapping
  ApiResponse<SupportTicketResponse> create(@Valid @RequestBody CreateTicketRequest req) {
    return ApiResponse.ok(service.create("PASSENGER", req));
  }

  @GetMapping
  ApiResponse<List<SupportTicketResponse>> list() {
    return ApiResponse.ok(service.listMine());
  }

  @GetMapping("/{ticketId}")
  ApiResponse<SupportTicketResponse> get(@PathVariable long ticketId) {
    return ApiResponse.ok(service.getMine(ticketId));
  }

  @PostMapping("/{ticketId}/messages")
  ApiResponse<SupportMessageResponse> addMessage(
      @PathVariable long ticketId, @Valid @RequestBody TicketMessageRequest req) {
    return ApiResponse.ok(service.addMessage("PASSENGER", ticketId, req));
  }
}
