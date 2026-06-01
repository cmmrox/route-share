package com.routeshare.passenger.repository;

import com.routeshare.passenger.dto.request.SavedPlaceRequest;
import com.routeshare.passenger.dto.response.SavedPlaceResponse;
import com.routeshare.passenger.entity.SavedPlaceEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SavedPlaceRepository extends JpaRepository<SavedPlaceEntity, Long> {
  @Query(
      value =
          """
      INSERT INTO passenger.saved_place(app_user_id, label, address, location)
      VALUES (:appUserId, :label, :address, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))
      RETURNING saved_place_id AS id, label, address, ST_Y(location) AS latitude, ST_X(location) AS longitude
      """,
      nativeQuery = true)
  SavedPlaceRow insertReturning(
      @Param("appUserId") long appUserId,
      @Param("label") String label,
      @Param("address") String address,
      @Param("lng") Double longitude,
      @Param("lat") Double latitude);

  @Query(
      value =
          """
      SELECT saved_place_id AS id, label, address, ST_Y(location) AS latitude, ST_X(location) AS longitude
      FROM passenger.saved_place
      WHERE app_user_id = :appUserId
      ORDER BY saved_place_id DESC
      """,
      nativeQuery = true)
  List<SavedPlaceRow> listRows(@Param("appUserId") long appUserId);

  @Query(
      value =
          """
      SELECT saved_place_id AS id, label, address, ST_Y(location) AS latitude, ST_X(location) AS longitude
      FROM passenger.saved_place
      WHERE saved_place_id = :savedPlaceId AND app_user_id = :appUserId
      """,
      nativeQuery = true)
  Optional<SavedPlaceRow> findRow(
      @Param("appUserId") long appUserId, @Param("savedPlaceId") long savedPlaceId);

  @Query(
      value =
          """
      UPDATE passenger.saved_place
      SET label = :label, address = :address, location = ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)
      WHERE saved_place_id = :savedPlaceId AND app_user_id = :appUserId
      RETURNING saved_place_id AS id, label, address, ST_Y(location) AS latitude, ST_X(location) AS longitude
      """,
      nativeQuery = true)
  Optional<SavedPlaceRow> updateReturning(
      @Param("appUserId") long appUserId,
      @Param("savedPlaceId") long savedPlaceId,
      @Param("label") String label,
      @Param("address") String address,
      @Param("lng") Double longitude,
      @Param("lat") Double latitude);

  @Transactional
  @Modifying
  long deleteByIdAndAppUserId(long id, long appUserId);

  default SavedPlaceResponse create(long appUserId, SavedPlaceRequest request) {
    return toResponse(
        insertReturning(
            appUserId,
            request.label(),
            request.address(),
            request.longitude(),
            request.latitude()));
  }

  default List<SavedPlaceResponse> list(long appUserId) {
    return listRows(appUserId).stream().map(this::toResponse).toList();
  }

  default Optional<SavedPlaceResponse> find(long appUserId, long savedPlaceId) {
    return findRow(appUserId, savedPlaceId).map(this::toResponse);
  }

  default Optional<SavedPlaceResponse> update(
      long appUserId, long savedPlaceId, SavedPlaceRequest request) {
    return updateReturning(
            appUserId,
            savedPlaceId,
            request.label(),
            request.address(),
            request.longitude(),
            request.latitude())
        .map(this::toResponse);
  }

  default boolean delete(long appUserId, long savedPlaceId) {
    return deleteByIdAndAppUserId(savedPlaceId, appUserId) > 0;
  }

  private SavedPlaceResponse toResponse(SavedPlaceRow row) {
    return new SavedPlaceResponse(
        row.getId(), row.getLabel(), row.getAddress(), row.getLatitude(), row.getLongitude());
  }

  interface SavedPlaceRow {
    long getId();

    String getLabel();

    String getAddress();

    double getLatitude();

    double getLongitude();
  }
}
