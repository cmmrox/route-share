package com.routeshare.identity.dto.response;

import java.util.UUID;

public record OtpRequestResponse(
    UUID verificationId, String phoneNumber, int expiresInSeconds, int resendAfterSeconds) {}
