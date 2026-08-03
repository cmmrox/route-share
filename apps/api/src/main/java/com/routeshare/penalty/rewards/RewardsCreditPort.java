package com.routeshare.penalty.rewards;

import java.math.BigDecimal;

/**
 * Where a passenger's half of somebody else's penalty goes.
 *
 * <p>P22 calls it ride credit, and slice 11 owns the shared rewards balance it lands in. This is
 * the seam so that neither slice waits for the other: the port is declared here, slice 11 replaces
 * the in-module implementation with the real balance, and nothing in the penalty flow changes.
 *
 * <p>The balance is role-neutral: passenger and driver compensation use the same auditable ledger.
 */
public interface RewardsCreditPort {

  /**
   * @param reference a stable, human-traceable origin such as {@code penalty:412}, so a balance the
   *     user queries can always be explained back to the event that produced it
   * @return the reference under which the credit was recorded
   */
  String credit(long appUserId, BigDecimal amount, String reference, String description);
}
