package com.routeshare.penalty.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * An admin's decision on a dispute.
 *
 * <p>{@code reverseAmount} is optional and defaults to the whole fee: a partial reversal exists for
 * the case where some of the fee was fair and some was not, which is a judgement only a person can
 * make.
 */
public record PenaltyDisputeDecisionRequest(
    @NotBlank @Pattern(regexp = "UPHELD|REVERSED") String decision,
    @Size(max = 2000) String note,
    @DecimalMin("0.00") BigDecimal reverseAmount) {}
