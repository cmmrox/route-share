package com.routeshare.driver.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentMetadataRequest(
    @NotBlank @Size(max = 80) String documentType, @NotBlank @Size(max = 500) String storageKey) {}
