package com.routeshare.identity.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.common.security.RouteShareRoles;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.dto.response.AuthMeResponse;
import com.routeshare.identity.repository.AppUserRepository;
import com.routeshare.identity.service.AuthMeService;
import com.routeshare.passenger.facade.PassengerFacade;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthMeServiceImpl implements AuthMeService {
  private final CurrentUserProvider currentUsers;
  private final AppUserRepository appUsers;
  private final PassengerFacade passengerFacade;
  private final DriverFacade driverFacade;

  public AuthMeResponse current() {
    var current = currentUsers.requireCurrentUser();
    var app = appUsers.upsertFromToken(current);
    boolean passenger = passengerFacade.existsPassengerProfileByAppUserId(app.appUserId());
    String driverStatus = driverFacade.findDriverStatusByAppUserId(app.appUserId()).orElse(null);
    boolean driver = driverStatus != null;
    Set<String> modes = new LinkedHashSet<>();
    if (passenger || current.roles().contains(RouteShareRoles.PASSENGER)) {
      modes.add("PASSENGER");
    }
    if (driver || current.roles().contains(RouteShareRoles.DRIVER)) {
      modes.add("DRIVER");
    }
    if (current.roles().stream()
        .anyMatch(role -> role.contains("ADMIN") || role.contains("AGENT"))) {
      modes.add("ADMIN");
    }
    return new AuthMeResponse(
        current.subject(),
        current.email(),
        current.phone(),
        current.displayName(),
        current.roles(),
        passenger,
        driver,
        driverStatus,
        modes);
  }
}
