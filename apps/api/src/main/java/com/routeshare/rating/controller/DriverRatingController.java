package com.routeshare.rating.controller;

import com.routeshare.common.security.DriverAccess;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.rating.dto.RatingSummaryResponse;
import com.routeshare.rating.service.RatingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@DriverAccess
public class DriverRatingController {
  private final RatingService service;

  public DriverRatingController(RatingService service) {
    this.service = service;
  }

  @GetMapping("/api/v1/driver/ratings")
  ApiResponse<RatingSummaryResponse> myRatings() {
    return ApiResponse.ok(service.myDriverRatings());
  }
}
