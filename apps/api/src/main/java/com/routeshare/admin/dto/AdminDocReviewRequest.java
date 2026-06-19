package com.routeshare.admin.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminDocReviewRequest(
    @Pattern(regexp = "APPROVE|REJECT") String decision, @Size(max = 500) String rejectionReason) {}
