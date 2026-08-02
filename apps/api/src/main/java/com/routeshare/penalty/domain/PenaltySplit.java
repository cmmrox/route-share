package com.routeshare.penalty.domain;

import java.math.BigDecimal;

/**
 * A fee and its two halves. The halves always re-add to the fee — see {@link PenaltyPolicy#split}.
 */
public record PenaltySplit(BigDecimal fee, BigDecimal victimShare, BigDecimal platformShare) {}
