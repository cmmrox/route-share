package com.routeshare.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
    @NotBlank @Size(max = 200) String subject,
    @Size(max = 80) String category,
    @Size(max = 20) String priority,
    @NotBlank @Size(max = 4000) String message) {}
