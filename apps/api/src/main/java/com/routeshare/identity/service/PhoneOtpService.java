package com.routeshare.identity.service;

import com.routeshare.identity.dto.request.OtpRequest;
import com.routeshare.identity.dto.request.OtpVerifyRequest;
import com.routeshare.identity.dto.response.OtpRequestResponse;
import com.routeshare.identity.dto.response.OtpVerifyResponse;

public interface PhoneOtpService {
  OtpRequestResponse requestOtp(OtpRequest request);

  OtpVerifyResponse verifyOtp(OtpVerifyRequest request);
}
