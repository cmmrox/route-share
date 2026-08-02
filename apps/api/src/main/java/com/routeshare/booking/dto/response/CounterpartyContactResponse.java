package com.routeshare.booking.dto.response;

/**
 * The counterparty's number, for a direct dial (D5).
 *
 * <p>Carries a first name and a number and nothing else. The emergency numbers travel with it
 * because they are static, always available, and deliberately outside every disclosure rule — a
 * passenger who cannot reach her driver must never also be unable to reach help.
 */
public record CounterpartyContactResponse(
    long bookingId,
    String role,
    String firstName,
    String phoneNumber,
    String emergencyNumber,
    String safetyLineNumber) {}
