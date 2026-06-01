package com.routeshare.passenger.service;

import com.routeshare.passenger.dto.request.SavedPlaceRequest;
import com.routeshare.passenger.dto.response.SavedPlaceResponse;
import java.util.List;

public interface SavedPlaceService {
  SavedPlaceResponse create(SavedPlaceRequest req);

  List<SavedPlaceResponse> listMine();

  SavedPlaceResponse get(long savedPlaceId);

  SavedPlaceResponse getMine(long savedPlaceId);

  SavedPlaceResponse update(long savedPlaceId, SavedPlaceRequest req);

  void delete(long savedPlaceId);
}
