package com.routeshare.passenger.dto.request;

import jakarta.validation.constraints.NotNull;

/** Confirms the bytes for one capture actually reached storage. */
public record VerificationStepSubmitRequest(@NotNull Long documentId) {}
