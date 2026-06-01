package com.routeshare.driver.service;

import com.routeshare.driver.dto.request.DriverApplicationRequest;
import com.routeshare.driver.dto.response.DriverProfileResponse;

public interface DriverService {
  DriverProfileResponse submit(DriverApplicationRequest req);

  DriverProfileResponse apply(DriverApplicationRequest req);

  java.util.Optional<DriverProfileResponse> get();

  java.util.Optional<DriverProfileResponse> mine();

  DriverProfileResponse review(long driverProfileId, String status);
}
