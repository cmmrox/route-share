package com.routeshare.penalty.rewards;

import com.routeshare.notification.facade.NotificationFacade;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * The temporary rewards balance, in force until slice 11 lands the real one.
 *
 * <p>It does not invent a balance table. The credit is already durable — {@code
 * penalty.penalty_beneficiary} holds the row, the amount and the person — so what is missing is
 * only the balance that aggregates such rows. Writing a second, throwaway ledger here would give
 * slice 11 a migration to unpick and a reconciliation to run.
 *
 * <p>So this tells the passenger her credit exists and returns the reference the beneficiary row is
 * stamped with. When slice 11 replaces this bean, every credit ever made can be replayed from those
 * rows, which is exactly the property a stub has to preserve.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PendingRewardsCreditAdapter implements RewardsCreditPort {

  private final NotificationFacade notifications;

  @Override
  public String credit(long appUserId, BigDecimal amount, String reference, String description) {
    notifications.notifyUser(
        appUserId,
        "PENALTY_COMPENSATION",
        "Ride credit added",
        description,
        Map.of("reference", reference, "amount", amount.toPlainString()));
    log.info(
        "rewards credit recorded appUserId={} amount={} reference={}",
        appUserId,
        amount,
        reference);
    return reference;
  }
}
