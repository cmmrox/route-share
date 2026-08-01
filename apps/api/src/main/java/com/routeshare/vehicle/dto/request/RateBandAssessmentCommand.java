package com.routeshare.vehicle.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * An admin's assessment. The band is typed directly (decision D2); the factors are the explanation
 * shown to the driver, not the inputs it was derived from.
 */
public record RateBandAssessmentCommand(
    @NotNull @Positive BigDecimal rateMin,
    @NotNull @Positive BigDecimal rateMax,
    @Valid @Size(max = 4) List<FactorCommand> factors,
    @Size(max = 2000) String note) {

  public record FactorCommand(
      @NotNull String key,
      @NotNull @Size(max = 120) String label,
      @Size(max = 500) String detail,
      @NotNull BigDecimal delta) {}
}
