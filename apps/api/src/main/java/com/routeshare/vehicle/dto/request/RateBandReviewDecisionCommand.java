package com.routeshare.vehicle.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * @param decision APPROVED writes the new band; REJECTED leaves the live band exactly as it was
 */
public record RateBandReviewDecisionCommand(
    @NotBlank @Pattern(regexp = "APPROVED|REJECTED") String decision,
    BigDecimal rateMin,
    BigDecimal rateMax,
    @Valid @Size(max = 4) List<RateBandAssessmentCommand.FactorCommand> factors,
    @Size(max = 2000) String note) {}
