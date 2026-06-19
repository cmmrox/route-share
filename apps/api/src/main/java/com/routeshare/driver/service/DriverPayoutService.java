package com.routeshare.driver.service;

import com.routeshare.driver.dto.request.PayoutProfileRequest;
import com.routeshare.driver.dto.response.PayoutProfileResponse;

public interface DriverPayoutService {
  PayoutProfileResponse getMine();

  PayoutProfileResponse saveMine(PayoutProfileRequest req);
}
