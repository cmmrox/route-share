package com.routeshare.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterPushRequest(
    @NotBlank @Pattern(regexp = "ANDROID|IOS|WEB") String platform, @NotBlank String token) {}
