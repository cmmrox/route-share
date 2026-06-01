package com.routeshare.vehicle.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VehicleDocumentRequest(
    @NotBlank @Size(max = 80) String documentType, @NotBlank @Size(max = 500) String storageKey) {}
