package com.routeshare.passenger.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * A reviewer's decision on one attempt.
 *
 * <p>{@code gender} is read off the NIC and written only on approval — it is the one place in the
 * system that value is ever set. Rejections name the steps that failed and why, because "try again"
 * with no indication of which of four images was wrong is a rider who will get it wrong again.
 */
public record VerificationDecisionRequest(
    @NotNull @Pattern(regexp = "APPROVED|REJECTED") String decision,
    @Pattern(regexp = "FEMALE|MALE|UNSPECIFIED") String gender,
    List<@Size(max = 40) String> rejectedSteps,
    @Size(max = 500) String note) {}
