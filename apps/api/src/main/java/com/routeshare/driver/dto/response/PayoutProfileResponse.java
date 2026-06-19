package com.routeshare.driver.dto.response;

import java.time.Instant;

/** Payout profile with sensitive account/wallet numbers masked to the last 4 digits. */
public record PayoutProfileResponse(
    boolean configured,
    String method,
    String bankName,
    String branch,
    String accountName,
    String accountNumberMasked,
    String walletProvider,
    String walletNumberMasked,
    String status,
    Instant updatedAt) {}
