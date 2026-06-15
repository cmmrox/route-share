package com.routeshare.identity.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.identity.dto.request.OtpRequest;
import com.routeshare.identity.dto.request.OtpVerifyRequest;
import com.routeshare.identity.dto.response.AuthMeResponse;
import com.routeshare.identity.dto.response.OtpRequestResponse;
import com.routeshare.identity.dto.response.OtpVerifyResponse;
import com.routeshare.identity.service.AuthMeService;
import com.routeshare.identity.service.PhoneOtpService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthMeService service;
  private final PhoneOtpService phoneOtpService;

  public AuthController(AuthMeService service, PhoneOtpService phoneOtpService) {
    this.service = service;
    this.phoneOtpService = phoneOtpService;
  }

  @GetMapping("/me")
  ApiResponse<AuthMeResponse> me() {
    return ApiResponse.ok(service.current());
  }

  @PostMapping("/otp/request")
  ApiResponse<OtpRequestResponse> requestOtp(@Valid @RequestBody OtpRequest request) {
    return ApiResponse.ok(phoneOtpService.requestOtp(request));
  }

  @PostMapping("/otp/verify")
  ApiResponse<OtpVerifyResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
    return ApiResponse.ok(phoneOtpService.verifyOtp(request));
  }
}
