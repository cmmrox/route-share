package com.routeshare.passenger.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.passenger.domain.PhotoVisibility;
import com.routeshare.passenger.dto.request.PhotoVisibilityRequest;
import com.routeshare.passenger.dto.response.PhotoVisibilityResponse;
import com.routeshare.passenger.repository.PassengerProfileRepository;
import com.routeshare.passenger.service.PhotoVisibilityService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhotoVisibilityServiceImpl implements PhotoVisibilityService {

  private final CurrentUserProvider current;
  private final IdentityFacade identity;
  private final PassengerProfileRepository profiles;

  @Override
  @Transactional
  public PhotoVisibilityResponse mine() {
    long appUserId = currentAppUserId();
    profiles.ensureExists(appUserId);
    return new PhotoVisibilityResponse(
        profiles
            .findRiderProfile(appUserId)
            .map(PassengerProfileRepository.RiderProfileRow::getPhotoVisibility)
            .orElse(PhotoVisibility.MATCHED.name()),
        PhotoVisibilityResponse.allOptions());
  }

  @Override
  @Transactional
  public PhotoVisibilityResponse update(PhotoVisibilityRequest request) {
    long appUserId = currentAppUserId();
    profiles.ensureExists(appUserId);
    var profile =
        profiles
            .findEntityByAppUserId(appUserId)
            .orElseThrow(() -> new IllegalStateException("Passenger profile could not be created"));
    profile.setPhotoVisibility(PhotoVisibility.of(request.visibility()).name());
    profiles.save(profile);
    return new PhotoVisibilityResponse(
        profile.getPhotoVisibility(), PhotoVisibilityResponse.allOptions());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<String> resolve(
      long viewerAppUserId, long subjectAppUserId, ViewContext context) {
    var row = profiles.findRiderProfile(subjectAppUserId);
    Optional<String> photo = row.map(PassengerProfileRepository.RiderProfileRow::getPhotoUrl);
    if (photo.isEmpty() || photo.get().isBlank()) {
      return Optional.empty();
    }

    if (viewerAppUserId == subjectAppUserId || context == ViewContext.SELF) {
      return photo;
    }

    // The asymmetry. A driver cannot hide from the rider who is about to get into his car — but he
    // is not on display to everyone who searches either, so the confirmed booking is what opens it.
    // Only ever reached when the caller says the subject is the driver on that booking: in a
    // unified app most drivers also ride, and inferring the role from the profile would show a
    // hidden photo to the driver of any rider who happens to drive too.
    if (context == ViewContext.CONFIRMED_BOOKING_DRIVER) {
      return photo;
    }

    PhotoVisibility visibility =
        PhotoVisibility.of(
            row.map(PassengerProfileRepository.RiderProfileRow::getPhotoVisibility).orElse(null));
    return switch (visibility) {
      case PUBLIC -> photo;
      case MATCHED -> context == ViewContext.CONFIRMED_BOOKING ? photo : Optional.empty();
        // Never, and never merely omitted by the client: a URL in a payload is a URL in a log, a
        // cache and a proxy, and none of those was part of the choice she made.
      case HIDDEN -> Optional.empty();
    };
  }

  private long currentAppUserId() {
    return identity.upsertFromToken(current.requireCurrentUser()).appUserId();
  }
}
