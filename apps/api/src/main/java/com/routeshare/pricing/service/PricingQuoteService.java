package com.routeshare.pricing.service;

import com.routeshare.pricing.dto.request.RouteFareEstimateRequest;
import com.routeshare.pricing.dto.response.RouteFareResponse;

public interface PricingQuoteService {
  /** Prices a segment of a published trip, entirely from server-held geometry and the rate band. */
  RouteFareResponse estimateByRoute(RouteFareEstimateRequest req);
}
