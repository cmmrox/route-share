package com.routeshare.common.security;

import com.routeshare.common.errors.GateDeniedException;
import com.routeshare.driver.domain.DriverGate;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.facade.IdentityFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * The composite driver gate, replacing a bare {@code hasRole('DRIVER')}.
 *
 * <p>A role check answers one question with a boolean and loses the reason. Driving depends on
 * three independent facts — the account is not suspended, an approved driver profile exists, and no
 * deactivation is open — which the prototype renders as five different screens. This guard resolves
 * all three and, when it refuses, throws the reason rather than returning {@code false}, so the 403
 * body carries the code and the screen to go to.
 *
 * <p>Order is load-bearing. Suspension is resolved first and outranks every driver gate: a
 * suspended driver must see S13 (the appeal route), never S08 ("we're reviewing you"), because only
 * one of those is true and only one of them is actionable.
 *
 * <p>Authorities are not consulted at all. The token's roles are already derived from this same
 * projection, so re-reading them would only add a way for a stale or forged claim to matter — a
 * fabricated {@code X-Mode} header or an OTP token minted before a deactivation changes nothing
 * here.
 */
@Component("driverGuard")
@RequiredArgsConstructor
public class DriverGuard {
  private final CurrentUserProvider currentUsers;
  private final IdentityFacade identity;
  private final DriverFacade drivers;

  /** True when the caller may use driver endpoints; otherwise throws the reason. */
  public boolean canDrive(Authentication authentication) {
    long appUserId = requireActiveAppUserId();
    return allow(drivers.gatesFor(appUserId));
  }

  /** True when the caller may publish a route; otherwise throws the reason. */
  public boolean canPublish(Authentication authentication) {
    long appUserId = requireActiveAppUserId();
    return allow(drivers.publishGatesFor(appUserId));
  }

  /**
   * True when the caller may reach their own driver account surfaces — earnings, payout details,
   * support — which a deactivation deliberately leaves open (D34).
   */
  public boolean canManageDriverAccount(Authentication authentication) {
    long appUserId = requireActiveAppUserId();
    if (drivers.findDriverProfileIdByAppUserId(appUserId).isPresent()) {
      return true;
    }
    return allow(drivers.gatesFor(appUserId));
  }

  /**
   * Resolves the caller through the ACTIVE guard, so a suspended account is refused here — with
   * {@code ACCOUNT_SUSPENDED} — before any driver gate is even computed.
   */
  private long requireActiveAppUserId() {
    return identity.upsertFromToken(currentUsers.requireCurrentUser()).appUserId();
  }

  private boolean allow(List<DriverGate> gates) {
    if (gates.isEmpty()) {
      return true;
    }
    DriverGate first = gates.get(0);
    throw new GateDeniedException(first.code(), first.message(), first.actionPath());
  }
}
