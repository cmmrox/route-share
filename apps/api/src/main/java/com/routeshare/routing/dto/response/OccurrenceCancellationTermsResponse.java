package com.routeshare.routing.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * D30: what cancelling this trip right now would cost, before he does it.
 *
 * <p>The percentage is this module's rule and the money is slice 06's arithmetic — the two must
 * never both know the rate, or a screen and a ledger will one day disagree about the same fee.
 */
public record OccurrenceCancellationTermsResponse(
    long routeOccurrenceId,
    BigDecimal hoursBeforeDeparture,
    boolean withinFreeWindow,
    int freeWindowHours,
    String explanation,
    BigDecimal penaltyPct,
    BigDecimal expectedNet,
    BigDecimal penaltyAmount,
    BigDecimal riderShare,
    BigDecimal platformShare,
    List<String> affectedRiderFirstNames,
    int affectedBookings,
    List<String> reasonCodes) {}
