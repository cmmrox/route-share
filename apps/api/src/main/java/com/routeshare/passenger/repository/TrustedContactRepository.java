package com.routeshare.passenger.repository;

import com.routeshare.passenger.dto.request.TrustedContactRequest;
import com.routeshare.passenger.dto.response.TrustedContactResponse;
import com.routeshare.passenger.entity.TrustedContactEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrustedContactRepository extends JpaRepository<TrustedContactEntity, Long> {
  List<TrustedContactEntity> findByAppUserIdOrderByIdDesc(long appUserId);

  Optional<TrustedContactEntity> findByIdAndAppUserId(long id, long appUserId);

  boolean existsByIdAndAppUserId(long id, long appUserId);

  default TrustedContactResponse create(long appUserId, TrustedContactRequest request) {
    return toResponse(
        save(
            new TrustedContactEntity(
                null, appUserId, request.name(), request.phone(), request.relationship(), true)));
  }

  default List<TrustedContactResponse> list(long appUserId) {
    return findByAppUserIdOrderByIdDesc(appUserId).stream().map(this::toResponse).toList();
  }

  default Optional<TrustedContactResponse> find(long appUserId, long contactId) {
    return findByIdAndAppUserId(contactId, appUserId).map(this::toResponse);
  }

  default Optional<TrustedContactResponse> update(
      long appUserId, long contactId, TrustedContactRequest request) {
    return findByIdAndAppUserId(contactId, appUserId)
        .map(
            entity -> {
              entity.setName(request.name());
              entity.setPhone(request.phone());
              entity.setRelationship(request.relationship());
              return toResponse(save(entity));
            });
  }

  default boolean delete(long appUserId, long contactId) {
    return findByIdAndAppUserId(contactId, appUserId)
        .map(
            entity -> {
              delete(entity);
              return true;
            })
        .orElse(false);
  }

  private TrustedContactResponse toResponse(TrustedContactEntity entity) {
    return new TrustedContactResponse(
        entity.getId(), entity.getName(), entity.getPhone(), entity.getRelationship());
  }
}
