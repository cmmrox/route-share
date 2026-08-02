package com.routeshare.admin.controller;

import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.routing.dto.request.PickupPointRequest;
import com.routeshare.routing.dto.response.PickupPointResponse;
import com.routeshare.routing.service.PickupPointService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The curated tier.
 *
 * <p>Worth an operator's time because it is the only tier that produces a real landmark name: a
 * derived point can only be labelled by its address, since a Places {@code displayName} is a
 * Pro-tier field and one Pro field re-prices the whole request. Curating the launch corridors is
 * therefore both the cheapest option and the best-quality one, which is rare enough to be worth
 * saying out loud.
 */
@RestController
@RequestMapping("/api/v1/admin/pickup-points")
@PreAuthorize("hasAnyRole('ADMIN','OPS_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminPickupPointController {

  private final PickupPointService pickupPoints;
  private final AdminAuditService audit;
  private final CurrentUserProvider currentUsers;
  private final IdentityFacade identity;

  @GetMapping
  ApiResponse<List<PickupPointResponse>> list(@RequestParam(required = false) String source) {
    return ApiResponse.ok(pickupPoints.list(source));
  }

  @PostMapping
  ApiResponse<PickupPointResponse> create(@Valid @RequestBody PickupPointRequest request) {
    long actor = identity.upsertFromToken(currentUsers.requireCurrentUser()).appUserId();
    var created = pickupPoints.createCurated(request, actor);
    audit.record(
        "PICKUP_POINT_CURATED",
        "pickup_point",
        String.valueOf(created.pickupPointId()),
        "{\"label\":\"" + request.label().replace("\"", "") + "\"}");
    return ApiResponse.ok(created);
  }
}
