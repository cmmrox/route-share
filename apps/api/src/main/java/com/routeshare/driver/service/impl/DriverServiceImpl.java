package com.routeshare.driver.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.common.security.RouteShareRoles;
import com.routeshare.driver.dto.request.DriverApplicationRequest;
import com.routeshare.driver.dto.response.DriverProfileResponse;
import com.routeshare.driver.mapper.DriverMapper;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.driver.service.DriverService;
import com.routeshare.identity.facade.IdentityFacade;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {
  private static final String APPROVED_STATUS = "APPROVED";

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final DriverProfileRepository drivers;
  private final DriverMapper driverMapper;

  @Transactional
  public DriverProfileResponse submit(DriverApplicationRequest req) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    drivers.submit(app.appUserId(), req.displayName());
    return get().orElseThrow();
  }

  @Transactional
  public DriverProfileResponse apply(DriverApplicationRequest req) {
    return submit(req);
  }

  public Optional<DriverProfileResponse> get() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return drivers.findByAppUserId(app.appUserId()).map(driverMapper::toProfileResponse);
  }

  public Optional<DriverProfileResponse> mine() {
    return get();
  }

  /**
   * Approval is where a rider becomes a driver, so it is also where the role is granted. Any other
   * outcome takes the role back: a profile that stops being approved must stop reaching driver
   * endpoints on the caller's very next request, not when a cached role set expires.
   */
  @Transactional
  public DriverProfileResponse review(long driverProfileId, String status) {
    var entity = drivers.findById(driverProfileId).orElseThrow();
    entity.setVerificationStatus(status);
    var saved = drivers.save(entity);
    if (APPROVED_STATUS.equals(status)) {
      identityFacade.grantRealmRole(saved.getAppUserId(), RouteShareRoles.DRIVER);
    } else {
      identityFacade.revokeRealmRole(saved.getAppUserId(), RouteShareRoles.DRIVER);
    }
    return driverMapper.toProfileResponse(saved);
  }
}
