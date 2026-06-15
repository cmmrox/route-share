package com.routeshare.identity.dto.response;

public record OtpVerifyResponse(
    boolean verified, String phoneNumber, String accessToken, long expiresInSeconds) {}
