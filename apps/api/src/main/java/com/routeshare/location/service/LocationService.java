package com.routeshare.location.service;

import com.routeshare.location.dto.request.LocationUpdateRequest;
import java.util.Map;

public interface LocationService {
  Map<String, Object> update(LocationUpdateRequest req);
}
