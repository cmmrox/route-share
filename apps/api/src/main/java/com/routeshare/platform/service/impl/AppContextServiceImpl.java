package com.routeshare.platform.service.impl;

import com.routeshare.booking.dto.response.PassengerBookingDetailResponse;
import com.routeshare.booking.service.BookingService;
import com.routeshare.common.errors.GateCodes;
import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.common.security.RouteShareRoles;
import com.routeshare.driver.domain.DriverGate;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.service.NotificationService;
import com.routeshare.passenger.facade.PassengerFacade;
import com.routeshare.passenger.service.PhotoVisibilityService;
import com.routeshare.platform.dto.response.ActiveModeResponse;
import com.routeshare.platform.dto.response.AppContextResponse;
import com.routeshare.platform.service.AppContextService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Composes the app shell's context from the modules that own each part, through their facades and
 * service interfaces only — no cross-module repository or entity access.
 *
 * <p>Two behaviours are deliberate and easy to get wrong:
 *
 * <ol>
 *   <li>A <b>suspended</b> caller is answered, not refused. Every business endpoint rejects a
 *       suspended account, but the shell has to render S13 with the reason and the appeal route, so
 *       this service resolves the user without the ACTIVE guard and reports the status as data.
 *   <li>Fields owned by later slices return <b>zero values, never null</b>, so the mobile shell is
 *       stable from day one and does not reshape as those slices land.
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppContextServiceImpl implements AppContextService {

  private static final String MODE_PASSENGER = "PASSENGER";
  private static final String MODE_DRIVER = "DRIVER";
  private static final String MODE_ADMIN = "ADMIN";
  private static final String STATUS_ACTIVE = "ACTIVE";
  private static final String STATUS_SUSPENDED = "SUSPENDED";
  private static final String CURRENCY = "LKR";

  private final CurrentUserProvider currentUsers;
  private final IdentityFacade identity;
  private final PassengerFacade passengers;
  private final PhotoVisibilityService photos;
  private final DriverFacade drivers;
  private final BookingService bookings;
  private final NotificationService notifications;
  private final com.routeshare.reliability.facade.ReliabilityFacade reliability;
  private final com.routeshare.platform.service.UserSettingsService userSettings;
  private final com.routeshare.rewards.facade.RewardsFacade rewards;
  private final Clock clock;

  @Override
  public AppContextResponse current() {
    CurrentUser token = currentUsers.requireCurrentUser();
    AppUser user = identity.upsertFromTokenAllowingSuspended(token);

    boolean suspended = !STATUS_ACTIVE.equalsIgnoreCase(user.localStatus());
    boolean hasPassengerProfile = passengers.existsPassengerProfileByAppUserId(user.appUserId());
    String driverStatus = drivers.findDriverStatusByAppUserId(user.appUserId()).orElse("NONE");

    return new AppContextResponse(
        token.subject(),
        token.displayName(),
        token.phone(),
        token.email(),
        photos
            .resolve(user.appUserId(), user.appUserId(), PhotoVisibilityService.ViewContext.SELF)
            .orElse(null),
        availableModes(token, user.appUserId(), hasPassengerProfile, driverStatus, suspended),
        activeModeDefault(user.appUserId(), suspended),
        driver(user.appUserId(), driverStatus, suspended),
        // The prepay flag is slice 05's, and it is read from the month's counter rather than
        // stored separately so there is one number and nothing to fall out of step with it.
        new AppContextResponse.Passenger(
            passengers.riderEligibilityProfile(user.appUserId()).verificationLevel(),
            passengers.photoVisibilityOf(user.appUserId()),
            reliability.prepayRequired(user.appUserId())),
        account(user, suspended),
        suspended ? null : activeTrip(),
        new AppContextResponse.Money(CURRENCY, BigDecimal.ZERO, rewards.balance(user.appUserId())),
        badges(suspended),
        settings(user.appUserId()),
        clock.instant());
  }

  private AppContextResponse.Settings settings(long appUserId) {
    var saved = userSettings.forAppUser(appUserId);
    return new AppContextResponse.Settings(saved.theme(), saved.language());
  }

  private Set<String> availableModes(
      CurrentUser token,
      long appUserId,
      boolean hasPassengerProfile,
      String driverStatus,
      boolean suspended) {
    Set<String> modes = new LinkedHashSet<>();
    if (hasPassengerProfile || token.roles().contains(RouteShareRoles.PASSENGER)) {
      modes.add(MODE_PASSENGER);
    }
    // A driver profile alone is not driver access, and neither is the role on its own: the mode
    // chip appears only when the account could actually drive right now.
    if (!suspended
        && "APPROVED".equalsIgnoreCase(driverStatus)
        && drivers.gatesFor(appUserId).isEmpty()) {
      modes.add(MODE_DRIVER);
    }
    if (token.roles().stream().anyMatch(r -> r.contains("ADMIN") || r.contains("AGENT"))) {
      modes.add(MODE_ADMIN);
    }
    if (modes.isEmpty()) {
      modes.add(MODE_PASSENGER);
    }
    return modes;
  }

  /**
   * Where the app reopens. A stored DRIVER preference is honoured only while driving is still
   * available — otherwise a driver deactivated overnight would cold-start into a mode that refuses
   * every call.
   */
  private String activeModeDefault(long appUserId, boolean suspended) {
    if (suspended) {
      return MODE_PASSENGER;
    }
    return identity
        .lastActiveMode(appUserId)
        .filter(mode -> !MODE_DRIVER.equals(mode) || drivers.gatesFor(appUserId).isEmpty())
        .orElse(MODE_PASSENGER);
  }

  private AppContextResponse.Driver driver(long appUserId, String driverStatus, boolean suspended) {
    // Suspension outranks every driver gate: S13 replaces S08/S09, it does not stack with them.
    if (suspended) {
      return new AppContextResponse.Driver(
          driverStatus,
          List.of(
              new AppContextResponse.Gate(
                  GateCodes.ACCOUNT_SUSPENDED,
                  "Your account is on hold. You can't book or publish trips while this is open.",
                  "/support")),
          false,
          false);
    }

    List<AppContextResponse.Gate> gates =
        drivers.gatesFor(appUserId).stream().map(AppContextServiceImpl::toGate).toList();
    boolean canPublish = gates.isEmpty() && drivers.publishGatesFor(appUserId).isEmpty();
    // S12 needs the publishing blockers listed too, not just the fact that publishing is blocked.
    if (gates.isEmpty() && !canPublish) {
      gates =
          drivers.publishGatesFor(appUserId).stream().map(AppContextServiceImpl::toGate).toList();
    }
    // D35 hides the toggle rather than showing one that always fails.
    return new AppContextResponse.Driver(
        driverStatus, gates, canPublish, drivers.canSetWomenOnly(appUserId));
  }

  private static AppContextResponse.Gate toGate(DriverGate gate) {
    return new AppContextResponse.Gate(gate.code(), gate.message(), gate.actionPath());
  }

  @Override
  public ActiveModeResponse setActiveMode(String mode) {
    CurrentUser token = currentUsers.requireCurrentUser();
    // The ACTIVE guard applies: a suspended account has no mode to switch into.
    AppUser user = identity.upsertFromToken(token);
    String requested = mode == null ? "" : mode.toUpperCase(Locale.ROOT);
    if (!MODE_PASSENGER.equals(requested) && !MODE_DRIVER.equals(requested)) {
      throw new IllegalArgumentException("mode must be PASSENGER or DRIVER");
    }
    if (MODE_DRIVER.equals(requested)) {
      // 409 rather than 403: the request is authorised, it conflicts with the account's state, and
      // the app already knows how to render the gate it gets back.
      drivers.gatesFor(user.appUserId()).stream()
          .findFirst()
          .ifPresent(
              gate -> {
                throw new GateConflictException(gate.code(), gate.message(), gate.actionPath());
              });
    }
    identity.setLastActiveMode(user.appUserId(), requested);
    return new ActiveModeResponse(requested);
  }

  private AppContextResponse.Account account(AppUser user, boolean suspended) {
    if (!suspended) {
      return new AppContextResponse.Account(STATUS_ACTIVE, null, null, null);
    }
    Optional<IdentityFacade.StatusChange> change = identity.latestStatusChange(user.appUserId());
    return new AppContextResponse.Account(
        STATUS_SUSPENDED,
        change.map(IdentityFacade.StatusChange::reason).orElse(null),
        change.map(IdentityFacade.StatusChange::caseRef).orElse(null),
        change.map(IdentityFacade.StatusChange::changedAt).orElse(null));
  }

  private AppContextResponse.ActiveTrip activeTrip() {
    // The shell must render even if one contributing module is unhealthy: a missing resume bar is a
    // far better failure than a shell that will not load at all.
    try {
      Optional<PassengerBookingDetailResponse> current = bookings.getCurrentPassengerTrip();
      if (current.isPresent()) {
        PassengerBookingDetailResponse b = current.get();
        return new AppContextResponse.ActiveTrip(
            "RIDING",
            b.bookingId(),
            b.tripId(),
            null,
            b.originLabel() + " → " + b.destinationLabel());
      }
    } catch (RuntimeException ex) {
      log.warn("app-context: active trip lookup failed, omitting resume bar", ex);
    }
    return null;
  }

  private AppContextResponse.Badges badges(boolean suspended) {
    if (suspended) {
      return new AppContextResponse.Badges(false, 0, 0, false);
    }
    int unread = 0;
    try {
      unread = (int) Math.min(Integer.MAX_VALUE, notifications.unreadCount());
    } catch (RuntimeException ex) {
      log.warn("app-context: unread count failed, badge omitted", ex);
    }
    // Trips count, home dot and account dot arrive with slices 07, 05 and 08 respectively.
    return new AppContextResponse.Badges(false, 0, unread, false);
  }
}
