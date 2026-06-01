package com.routeshare.passenger.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.passenger.dto.request.TrustedContactRequest;
import com.routeshare.passenger.dto.response.TrustedContactResponse;
import com.routeshare.passenger.mapper.PassengerMapper;
import com.routeshare.passenger.repository.TrustedContactRepository;
import com.routeshare.passenger.service.TrustedContactService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrustedContactServiceImpl implements TrustedContactService {
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final TrustedContactRepository trustedContacts;
  private final PassengerMapper passengerMapper;

  @Transactional
  public TrustedContactResponse create(TrustedContactRequest req) {
    long appUserId = currentAppUserId();
    return passengerMapper.toTrustedContactResponse(
        trustedContacts.save(passengerMapper.toTrustedContactEntity(appUserId, req)));
  }

  public List<TrustedContactResponse> listMine() {
    return trustedContacts.findByAppUserIdOrderByIdDesc(currentAppUserId()).stream()
        .map(passengerMapper::toTrustedContactResponse)
        .toList();
  }

  public TrustedContactResponse get(long trustedContactId) {
    return trustedContacts
        .findByIdAndAppUserId(trustedContactId, currentAppUserId())
        .map(passengerMapper::toTrustedContactResponse)
        .orElseThrow(
            () -> new AccessDeniedException("Trusted contact does not belong to current user"));
  }

  public TrustedContactResponse getMine(long trustedContactId) {
    return get(trustedContactId);
  }

  @Transactional
  public TrustedContactResponse update(long trustedContactId, TrustedContactRequest req) {
    return trustedContacts
        .findByIdAndAppUserId(trustedContactId, currentAppUserId())
        .map(
            entity -> {
              entity.setName(req.name());
              entity.setPhone(req.phone());
              entity.setRelationship(req.relationship());
              return passengerMapper.toTrustedContactResponse(trustedContacts.save(entity));
            })
        .orElseThrow(
            () -> new AccessDeniedException("Trusted contact does not belong to current user"));
  }

  @Transactional
  public void delete(long trustedContactId) {
    trustedContacts
        .findByIdAndAppUserId(trustedContactId, currentAppUserId())
        .ifPresentOrElse(
            trustedContacts::delete,
            () -> {
              throw new AccessDeniedException("Trusted contact does not belong to current user");
            });
  }

  private long currentAppUserId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }
}
