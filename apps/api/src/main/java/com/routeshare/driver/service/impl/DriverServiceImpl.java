package com.routeshare.driver.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
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

  @Transactional
  public DriverProfileResponse review(long driverProfileId, String status) {
    var entity = drivers.findById(driverProfileId).orElseThrow();
    entity.setVerificationStatus(status);
    return driverMapper.toProfileResponse(drivers.save(entity));
  }
}
