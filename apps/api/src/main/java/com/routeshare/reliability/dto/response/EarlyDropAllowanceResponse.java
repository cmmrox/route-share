package com.routeshare.reliability.dto.response;

import java.time.LocalDate;

/**
 * P16 / P16b: how many fare-adjusted early drops she has left this calendar month.
 *
 * <p>Exhaustion is data, not an error. The third drop still releases the seat — she is getting out
 * of the car either way — it simply is not repriced, and the screen has to be able to say so
 * <em>before</em> she taps rather than after she has been charged the full fare.
 */
public record EarlyDropAllowanceResponse(
    LocalDate month, int used, int allowance, int remaining, boolean nextDropWillBeAdjusted) {}
