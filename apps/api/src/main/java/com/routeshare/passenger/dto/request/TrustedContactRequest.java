package com.routeshare.passenger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrustedContactRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 40) String phone,
    @Size(max = 80) String relationship) {}
