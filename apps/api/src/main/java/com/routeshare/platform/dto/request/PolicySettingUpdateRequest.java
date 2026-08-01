package com.routeshare.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PolicySettingUpdateRequest(@NotBlank @Size(max = 200) String value) {}
