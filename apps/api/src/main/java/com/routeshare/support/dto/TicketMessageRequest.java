package com.routeshare.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketMessageRequest(@NotBlank @Size(max = 4000) String body) {}
