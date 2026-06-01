package com.routeshare.routing.service;

import com.routeshare.routing.dto.request.CoordinateRequest;
import com.routeshare.routing.dto.request.RoutePublishRequest;
import com.routeshare.routing.dto.request.RouteSearchRequest;
import com.routeshare.routing.dto.response.RouteSearchResponse;
import java.util.List;
import java.util.Map;

public interface RouteService {
  Map<String, Object> publish(RoutePublishRequest req);

  List<RouteSearchResponse> search(RouteSearchRequest req);

  void validateCoordinates(List<CoordinateRequest> coordinates);
}
