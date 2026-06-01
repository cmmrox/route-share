package com.routeshare.passenger.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.passenger.dto.request.SavedPlaceRequest;
import com.routeshare.passenger.dto.response.SavedPlaceResponse;
import com.routeshare.passenger.service.SavedPlaceService;
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
@RequestMapping("/api/v1/passenger/saved-places")
@PreAuthorize("isAuthenticated()")
public class SavedPlaceController {
  private final SavedPlaceService service;

  public SavedPlaceController(SavedPlaceService service) {
    this.service = service;
  }

  @PostMapping
  ApiResponse<SavedPlaceResponse> create(@Valid @RequestBody SavedPlaceRequest req) {
    return ApiResponse.ok(service.create(req));
  }

  @GetMapping
  ApiResponse<List<SavedPlaceResponse>> list() {
    return ApiResponse.ok(service.listMine());
  }

  @GetMapping("/{id}")
  ApiResponse<SavedPlaceResponse> get(@PathVariable long id) {
    return ApiResponse.ok(service.getMine(id));
  }

  @PutMapping("/{id}")
  ApiResponse<SavedPlaceResponse> update(
      @PathVariable long id, @Valid @RequestBody SavedPlaceRequest req) {
    return ApiResponse.ok(service.update(id, req));
  }

  @DeleteMapping("/{id}")
  ApiResponse<Map<String, Object>> delete(@PathVariable long id) {
    service.delete(id);
    return ApiResponse.ok(Map.of("deleted", true));
  }
}
