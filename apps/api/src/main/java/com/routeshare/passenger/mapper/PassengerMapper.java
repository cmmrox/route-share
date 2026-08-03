package com.routeshare.passenger.mapper;

import com.routeshare.common.mapper.RouteShareMapperConfig;
import com.routeshare.passenger.dto.request.TrustedContactRequest;
import com.routeshare.passenger.dto.response.PassengerProfileResponse;
import com.routeshare.passenger.dto.response.SavedPlaceResponse;
import com.routeshare.passenger.dto.response.TrustedContactResponse;
import com.routeshare.passenger.entity.TrustedContactEntity;
import com.routeshare.passenger.repository.PassengerProfileRepository.PassengerProfileRow;
import com.routeshare.passenger.repository.SavedPlaceRepository.SavedPlaceRow;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = RouteShareMapperConfig.class)
public interface PassengerMapper {
  SavedPlaceResponse toSavedPlaceResponse(SavedPlaceRow row);

  TrustedContactResponse toTrustedContactResponse(TrustedContactEntity entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "appUserId", source = "appUserId")
  @Mapping(target = "name", source = "request.name")
  @Mapping(target = "phone", source = "request.phone")
  @Mapping(target = "relationship", source = "request.relationship")
  @Mapping(target = "autoShareSos", constant = "true")
  TrustedContactEntity toTrustedContactEntity(long appUserId, TrustedContactRequest request);

  default PassengerProfileResponse toPassengerProfileResponse(
      PassengerProfileRow row, Map<String, Object> preferences) {
    return new PassengerProfileResponse(row.id(), row.fullName(), row.photoUrl(), preferences);
  }
}
