package com.routeshare.routing.dto.response;

import com.routeshare.maps.dto.CoordinateResponse;
import java.util.List;

/**
 * Road-following geometry of the segment a passenger travels on a published route, served from the
 * stored route line — the driver's actual route — with no billable maps-provider call.
 */
public record RouteGeometryResponse(
    List<CoordinateResponse> coordinates, long distanceMeters, String source) {}
