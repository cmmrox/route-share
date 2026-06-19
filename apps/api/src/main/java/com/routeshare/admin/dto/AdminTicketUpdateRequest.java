package com.routeshare.admin.dto;

import jakarta.validation.constraints.Pattern;

public record AdminTicketUpdateRequest(
    @Pattern(regexp = "OPEN|PENDING|RESOLVED|CLOSED") String status) {}
