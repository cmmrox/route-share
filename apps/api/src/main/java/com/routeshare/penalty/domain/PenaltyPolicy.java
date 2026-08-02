package com.routeshare.penalty.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * The penalty arithmetic, computed. Pure: every input is a parameter, so each figure the prototype
 * states can be reproduced without a database, a clock or a request.
 *
 * <pre>
 *   feeAmount     = round(base × ratePercent / 100)
 *   victimShare   = round(feeAmount × victimPercent / 100)
 *   platformShare = feeAmount − victimShare
 * </pre>
 *
 * <p><b>The platform's half is a subtraction, never a second rounding.</b> Rounding both halves
 * independently and hoping they add back is how a ledger creates or destroys a rupee per penalty;
 * subtraction makes {@code victimShare + platformShare = feeAmount} true by construction, and the
 * database asserts it too.
 *
 * <p>Money is rounded to whole rupees and carried at scale 2, exactly as {@code FareEngine} does —
 * every figure the screens show is a whole rupee, and a fee reading "LKR 49.25" is a number nobody
 * can hand over.
 */
public final class PenaltyPolicy {
  private static final int MONEY_SCALE = 2;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private PenaltyPolicy() {}

  /** {@code round(base × percent / 100)}, floored at zero. */
  public static BigDecimal fee(BigDecimal base, BigDecimal percent) {
    if (base == null || base.signum() <= 0 || percent == null || percent.signum() <= 0) {
      return money(BigDecimal.ZERO);
    }
    return money(base.multiply(percent).divide(HUNDRED, 6, RoundingMode.HALF_UP));
  }

  /**
   * Splits a fee between the person it let down and the platform.
   *
   * @param victimPercent normally 50; read from policy so the split is tunable without a deployment
   */
  public static PenaltySplit split(BigDecimal fee, BigDecimal victimPercent) {
    BigDecimal total = money(fee == null ? BigDecimal.ZERO : fee);
    if (total.signum() <= 0) {
      return new PenaltySplit(
          money(BigDecimal.ZERO), money(BigDecimal.ZERO), money(BigDecimal.ZERO));
    }
    BigDecimal percent = victimPercent == null ? BigDecimal.ZERO : victimPercent;
    BigDecimal victim = money(total.multiply(percent).divide(HUNDRED, 6, RoundingMode.HALF_UP));
    if (victim.compareTo(total) > 0) {
      victim = total;
    }
    return new PenaltySplit(total, victim, total.subtract(victim));
  }

  /**
   * Divides the victim half between several people who were all let down by the same act (D31: the
   * driver's penalty is shared "between them as ride credit").
   *
   * <p>Each gets a whole rupee and the remainder goes to the first, so the parts total the victim
   * share exactly and the allocation does not depend on iteration order. The caller supplies the
   * beneficiaries already sorted by booking id, which is what makes it deterministic.
   *
   * @return one amount per beneficiary, in the order given
   */
  public static List<BigDecimal> distribute(BigDecimal victimShare, int beneficiaries) {
    if (beneficiaries <= 0) {
      return List.of();
    }
    BigDecimal total = money(victimShare == null ? BigDecimal.ZERO : victimShare);
    BigDecimal count = BigDecimal.valueOf(beneficiaries);
    // Whole rupees, rounded down, so the remainder is never negative and can only be added.
    BigDecimal each = total.divide(count, 0, RoundingMode.DOWN).setScale(MONEY_SCALE);
    BigDecimal remainder = total.subtract(each.multiply(count));

    List<BigDecimal> amounts = new ArrayList<>(beneficiaries);
    for (int i = 0; i < beneficiaries; i++) {
      amounts.add(i == 0 ? each.add(remainder) : each);
    }
    return List.copyOf(amounts);
  }

  private static BigDecimal money(BigDecimal value) {
    return value.setScale(0, RoundingMode.HALF_UP).setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
  }
}
