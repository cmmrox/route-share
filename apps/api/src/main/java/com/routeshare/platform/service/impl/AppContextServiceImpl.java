package com.routeshare.platform.service.impl;

import com.routeshare.booking.dto.response.PassengerBookingDetailResponse;
import com.routeshare.booking.service.BookingService;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.common.security.RouteShareRoles;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.service.NotificationService;
import com.routeshare.passenger.facade.PassengerFacade;
import com.routeshare.platform.dto.response.AppContextResponse;
import com.routeshare.platform.service.AppContextService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
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
  private final DriverFacade drivers;
  private final BookingService bookings;
  private final NotificationService notifications;
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
        null, // photo URL arrives with slice 08's visibility rules
        availableModes(token, hasPassengerProfile, driverStatus),
        MODE_PASSENGER, // last-used mode is persisted in slice 01
        driver(driverStatus, suspended),
        new AppContextResponse.Passenger("NONE", "MATCHED", false), // slice 08
        account(user, suspended),
        suspended ? null : activeTrip(),
        new AppContextResponse.Money(CURRENCY, BigDecimal.ZERO, BigDecimal.ZERO), // slices 06, 11
        badges(suspended),
        new AppContextResponse.Settings("SYSTEM", "en"), // slice 10
        clock.instant());
  }

  private Set<String> availableModes(
      CurrentUser token, boolean hasPassengerProfile, String driverStatus) {
    Set<String> modes = new LinkedHashSet<>();
    if (hasPassengerProfile || token.roles().contains(RouteShareRoles.PASSENGER)) {
      modes.add(MODE_PASSENGER);
    }
    // A driver profile alone is not driver access: only an approved one puts the mode chip in
    // reach.
    if ("APPROVED".equalsIgnoreCase(driverStatus)
        && token.roles().contains(RouteShareRoles.DRIVER)) {
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

  private AppContextResponse.Driver driver(String driverStatus, boolean suspended) {
    List<AppContextResponse.Gate> gates = new ArrayList<>();

    // Suspension outranks every driver gate: S13 replaces S08/S09, it does not stack with them.
    if (suspended) {
      gates.add(
          new AppContextResponse.Gate(
              "ACCOUNT_SUSPENDED",
              "Your account is on hold. You can't book or publish trips while this is open.",
              "/support"));
      return new AppContextResponse.Driver(driverStatus, List.copyOf(gates), false, false);
    }

    switch (driverStatus == null ? "NONE" : driverStatus.toUpperCase(Locale.ROOT)) {
      case "APPROVED" -> {
        /* no gate — the real publish gate (documents, vehicle, rate band) lands in slices 01 and 02 */
      }
      case "PENDING_REVIEW", "SUBMITTED" ->
          gates.add(
              new AppContextResponse.Gate(
                  "DRIVER_REVIEW_PENDING",
                  "We're checking your documents. Usually done within one working day.",
                  "/driver/verification-status"));
      case "REJECTED" ->
          gates.add(
              new AppContextResponse.Gate(
                  "DRIVER_APPLICATION_REJECTED",
                  "One of your documents needs redoing.",
                  "/driver/verification-status"));
      case "SUSPENDED" ->
          gates.add(
              new AppContextResponse.Gate(
                  "DRIVER_DEACTIVATED",
                  "Your driver profile is deactivated. You can still ride as a passenger.",
                  "/driver/reinstatement-requests"));
      default ->
          gates.add(
              new AppContextResponse.Gate(
                  "DRIVER_PROFILE_MISSING",
                  "Publish the trips you already make and let riders book the empty seats.",
                  "/driver/application"));
    }

    boolean approved = "APPROVED".equalsIgnoreCase(driverStatus);
    // canPublish tightens in slice 01 (documents, vehicle) and slice 02 (rate band).
    return new AppContextResponse.Driver(driverStatus, List.copyOf(gates), approved, false);
  }

  private AppContextResponse.Account account(AppUser user, boolean suspended) {
    if (!suspended) {
      return new AppContextResponse.Account(STATUS_ACTIVE, null, null, null);
    }
    Optional<IdentityFacade.StatusChange> change = identity.latestStatusChange(user.appUserId());
    return new AppContextResponse.Account(
        STATUS_SUSPENDED,
        change.map(IdentityFacade.StatusChange::reason).orElse(null),
        change.map(c -> "SL-" + user.appUserId()).orElse(null),
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
