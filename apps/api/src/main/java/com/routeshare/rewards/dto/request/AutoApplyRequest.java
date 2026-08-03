package com.routeshare.rewards.dto.request;

import jakarta.validation.constraints.NotNull;

public record AutoApplyRequest(@NotNull Boolean enabled) {}
