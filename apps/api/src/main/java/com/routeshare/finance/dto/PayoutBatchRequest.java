package com.routeshare.finance.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public record PayoutBatchRequest(String note, @NotEmpty List<Item> items) {
  public record Item(
      @NotNull Long driverAppUserId, @NotNull @Positive BigDecimal amount, String currency) {}
}
