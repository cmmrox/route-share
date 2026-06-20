package com.routeshare.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BroadcastRequest(
    @NotBlank @Size(max = 120) String title, @NotBlank @Size(max = 1000) String body) {}
