package com.routeshare.vehicle.mapper;

import com.routeshare.common.mapper.RouteShareMapperConfig;
import com.routeshare.vehicle.dto.request.VehicleRequest;
import com.routeshare.vehicle.dto.response.VehicleDocumentResponse;
import com.routeshare.vehicle.dto.response.VehicleResponse;
import com.routeshare.vehicle.entity.VehicleDocumentEntity;
import com.routeshare.vehicle.entity.VehicleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = RouteShareMapperConfig.class)
public interface VehicleMapper {
  VehicleResponse toResponse(VehicleEntity entity);

  VehicleDocumentResponse toDocumentResponse(VehicleDocumentEntity entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "driverProfileId", source = "driverProfileId")
  @Mapping(target = "make", source = "request.make")
  @Mapping(target = "model", source = "request.model")
  @Mapping(target = "manufactureYear", source = "request.manufactureYear")
  @Mapping(target = "color", source = "request.color")
  @Mapping(target = "registrationNumber", source = "request.registrationNumber")
  @Mapping(target = "seatCount", source = "request.seatCount")
  @Mapping(target = "status", ignore = true)
  VehicleEntity toEntity(long driverProfileId, VehicleRequest request);
}
