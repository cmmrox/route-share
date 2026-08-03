package com.routeshare.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserSettingsRequest(
    @NotBlank @Pattern(regexp = "SYSTEM|LIGHT|DARK") String theme,
    @NotBlank @Pattern(regexp = "en|si|ta") String language,
    boolean shareLiveLocation,
    boolean showRatingPublicly,
    boolean receiptsByEmail) {}
