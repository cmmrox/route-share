package com.routeshare.notification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdatePreferencesRequest(@NotNull List<@Valid Category> categories) {
  public record Category(
      @NotBlank String key, boolean enabled, boolean push, boolean sms, boolean inApp) {}
}
