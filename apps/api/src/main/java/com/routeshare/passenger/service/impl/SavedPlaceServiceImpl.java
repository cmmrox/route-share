package com.routeshare.passenger.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.passenger.dto.request.SavedPlaceRequest;
import com.routeshare.passenger.dto.response.SavedPlaceResponse;
import com.routeshare.passenger.mapper.PassengerMapper;
import com.routeshare.passenger.repository.SavedPlaceRepository;
import com.routeshare.passenger.service.SavedPlaceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SavedPlaceServiceImpl implements SavedPlaceService {
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final SavedPlaceRepository savedPlaces;
  private final PassengerMapper passengerMapper;

  @Transactional
  public SavedPlaceResponse create(SavedPlaceRequest req) {
    return passengerMapper.toSavedPlaceResponse(
        savedPlaces.insertReturning(
            currentAppUserId(), req.label(), req.address(), req.longitude(), req.latitude()));
  }

  public List<SavedPlaceResponse> listMine() {
    return savedPlaces.listRows(currentAppUserId()).stream()
        .map(passengerMapper::toSavedPlaceResponse)
        .toList();
  }

  public SavedPlaceResponse get(long savedPlaceId) {
    return savedPlaces
        .findRow(currentAppUserId(), savedPlaceId)
        .map(passengerMapper::toSavedPlaceResponse)
        .orElseThrow(
            () -> new AccessDeniedException("Saved place does not belong to current user"));
  }

  public SavedPlaceResponse getMine(long savedPlaceId) {
    return get(savedPlaceId);
  }

  @Transactional
  public SavedPlaceResponse update(long savedPlaceId, SavedPlaceRequest req) {
    return savedPlaces
        .updateReturning(
            currentAppUserId(),
            savedPlaceId,
            req.label(),
            req.address(),
            req.longitude(),
            req.latitude())
        .map(passengerMapper::toSavedPlaceResponse)
        .orElseThrow(
            () -> new AccessDeniedException("Saved place does not belong to current user"));
  }

  @Transactional
  public void delete(long savedPlaceId) {
    if (savedPlaces.deleteByIdAndAppUserId(savedPlaceId, currentAppUserId()) <= 0) {
      throw new AccessDeniedException("Saved place does not belong to current user");
    }
  }

  private long currentAppUserId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }
}
