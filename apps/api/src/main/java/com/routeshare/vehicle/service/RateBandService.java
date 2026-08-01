package com.routeshare.vehicle.service;

import com.routeshare.vehicle.dto.request.RateBandAssessmentCommand;
import com.routeshare.vehicle.dto.request.RateBandReviewDecisionCommand;
import com.routeshare.vehicle.dto.request.RateBandReviewRequestCommand;
import com.routeshare.vehicle.dto.response.RateBandResponse;
import com.routeshare.vehicle.dto.response.RateBandReviewRequestResponse;
import com.routeshare.vehicle.dto.response.VehicleClassResponse;
import java.math.BigDecimal;
import java.util.List;

/**
 * The rate band: assessed by ComiGo, chosen within by the driver, and never typed freely by either.
 *
 * <p>Two rules separate the roles and are worth stating once. An <b>admin</b> sets the band and may
 * not choose the rate; a <b>driver</b> chooses the rate and may not touch the band. A driver who
 * could set their own band would be setting their own price, which is the single most valuable
 * privilege escalation in the system.
 */
public interface RateBandService {
  /** The classes offered on D07, with their seat caps and default ranges. */
  List<VehicleClassResponse> vehicleClasses();

  /** The caller's own vehicle's band. Refuses a vehicle the caller does not own. */
  RateBandResponse myBand(long vehicleId);

  /** Driver picks a rate inside the band. Outside it, or with no live band, is refused. */
  RateBandResponse chooseRate(long vehicleId, BigDecimal ratePerKm);

  /** One open re-assessment per vehicle; the live band keeps working while it is open. */
  RateBandReviewRequestResponse requestReview(long vehicleId, RateBandReviewRequestCommand cmd);

  List<RateBandReviewRequestResponse> myReviewRequests(long vehicleId);

  /** Admin view — same payload, without the ownership check. */
  RateBandResponse bandFor(long vehicleId);

  /** Admin sets the band and its justification. Moves the band to ACTIVE. */
  RateBandResponse assess(long vehicleId, RateBandAssessmentCommand cmd, long actorAppUserId);

  List<RateBandReviewRequestResponse> reviewRequests(String status);

  RateBandReviewRequestResponse decideReview(
      long requestId, RateBandReviewDecisionCommand cmd, long actorAppUserId);

  /**
   * Creates the PENDING_ASSESSMENT row a newly approved vehicle needs, so D40 has something to
   * show.
   */
  void ensureBandExists(long vehicleId);
}
