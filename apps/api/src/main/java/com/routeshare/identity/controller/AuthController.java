package com.routeshare.identity.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.identity.dto.response.AuthMeResponse;
import com.routeshare.identity.service.AuthMeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthMeService service;

  public AuthController(AuthMeService service) {
    this.service = service;
  }

  @GetMapping("/me")
  ApiResponse<AuthMeResponse> me() {
    return ApiResponse.ok(service.current());
  }
}
