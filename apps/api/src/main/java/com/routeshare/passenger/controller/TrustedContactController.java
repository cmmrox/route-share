package com.routeshare.passenger.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.passenger.dto.request.TrustedContactRequest;
import com.routeshare.passenger.dto.response.TrustedContactResponse;
import com.routeshare.passenger.service.TrustedContactService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/passenger/trusted-contacts")
@PreAuthorize("isAuthenticated()")
public class TrustedContactController {
  private final TrustedContactService service;

  public TrustedContactController(TrustedContactService service) {
    this.service = service;
  }

  @PostMapping
  ApiResponse<TrustedContactResponse> create(@Valid @RequestBody TrustedContactRequest req) {
    return ApiResponse.ok(service.create(req));
  }

  @GetMapping
  ApiResponse<List<TrustedContactResponse>> list() {
    return ApiResponse.ok(service.listMine());
  }

  @GetMapping("/{id}")
  ApiResponse<TrustedContactResponse> get(@PathVariable long id) {
    return ApiResponse.ok(service.getMine(id));
  }

  @PutMapping("/{id}")
  ApiResponse<TrustedContactResponse> update(
      @PathVariable long id, @Valid @RequestBody TrustedContactRequest req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/{id}")
  ApiResponse<Map<String, Object>> delete(@PathVariable long id) {
    service.delete(id);
    return ApiResponse.ok(Map.of("deleted", true));
  }
}
