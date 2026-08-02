package com.routeshare.routing.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.routing.dto.request.ApprovalModeRequest;
import com.routeshare.routing.dto.request.OccurrenceCancellationRequest;
import com.routeshare.routing.dto.response.AlternativeTripResponse;
import com.routeshare.routing.dto.response.OccurrenceCancellationTermsResponse;
import com.routeshare.routing.dto.response.OccurrenceEditabilityResponse;
import com.routeshare.routing.dto.response.SeatMapResponse;
import com.routeshare.routing.service.OccurrenceLifecycleService;
import com.routeshare.routing.service.SeatInventoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * P08's seat picker, and D09/D13/D30 on the driver's side.
 *
 * <p>The seat map is readable by any signed-in rider — it is inventory on a published trip, which
 * is what a search result already tells them. Everything that changes the trip is the driver's own
 * and checks ownership rather than merely the DRIVER role.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OccurrenceSeatController {

  private final SeatInventoryService seats;
  private final OccurrenceLifecycleService lifecycle;

  /** P08. */
  @GetMapping("/passenger/route-occurrences/{routeOccurrenceId}/seats")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<SeatMapResponse> seatMap(@PathVariable long routeOccurrenceId) {
    return ApiResponse.ok(seats.seatMap(routeOccurrenceId));
  }

  /** P13, P22, P24 — what to offer when a trip falls through. */
  @GetMapping("/passenger/route-occurrences/{routeOccurrenceId}/alternatives")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<List<AlternativeTripResponse>> alternatives(@PathVariable long routeOccurrenceId) {
    return ApiResponse.ok(lifecycle.alternatives(routeOccurrenceId));
  }

  /** D13. */
  @PutMapping("/driver/route-occurrences/{routeOccurrenceId}/approval-mode")
  @PreAuthorize("hasAnyRole('DRIVER','ADMIN','SUPER_ADMIN')")
  ApiResponse<Map<String, Object>> setApprovalMode(
      @PathVariable long routeOccurrenceId, @Valid @RequestBody ApprovalModeRequest request) {
    return ApiResponse.ok(lifecycle.setApprovalMode(routeOccurrenceId, request));
  }

  /** D09's banner. */
  @GetMapping("/driver/route-occurrences/{routeOccurrenceId}/editability")
  @PreAuthorize("hasAnyRole('DRIVER','ADMIN','SUPER_ADMIN')")
  ApiResponse<OccurrenceEditabilityResponse> editability(@PathVariable long routeOccurrenceId) {
    return ApiResponse.ok(lifecycle.editability(routeOccurrenceId));
  }

  /** D30, before he commits to it. */
  @GetMapping("/driver/route-occurrences/{routeOccurrenceId}/cancellation-terms")
  @PreAuthorize("hasAnyRole('DRIVER','ADMIN','SUPER_ADMIN')")
  ApiResponse<OccurrenceCancellationTermsResponse> cancellationTerms(
      @PathVariable long routeOccurrenceId) {
    return ApiResponse.ok(lifecycle.cancellationTerms(routeOccurrenceId));
  }

  /** D30/D31. */
  @PostMapping("/driver/route-occurrences/{routeOccurrenceId}/cancel")
  @PreAuthorize("hasAnyRole('DRIVER','ADMIN','SUPER_ADMIN')")
  ApiResponse<Map<String, Object>> cancel(
      @PathVariable long routeOccurrenceId,
      @Valid @RequestBody OccurrenceCancellationRequest request) {
    return ApiResponse.ok(lifecycle.cancel(routeOccurrenceId, request));
  }
}
