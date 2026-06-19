package com.routeshare.driver.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Driver payout details. For {@code BANK_TRANSFER}, bank/account fields are required; for {@code
 * MOBILE_WALLET}, the wallet fields are. Saving resets the profile to PENDING_VERIFICATION.
 */
public record PayoutProfileRequest(
    @NotBlank @Pattern(regexp = "BANK_TRANSFER|MOBILE_WALLET") String method,
    @Size(max = 120) String bankName,
    @Size(max = 120) String branch,
    @Size(max = 120) String accountName,
    @Size(max = 40) String accountNumber,
    @Size(max = 60) String walletProvider,
    @Size(max = 40) String walletNumber) {}
