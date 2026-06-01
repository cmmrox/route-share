package com.routeshare.driver.mapper;

import com.routeshare.common.mapper.RouteShareMapperConfig;
import com.routeshare.driver.dto.request.DocumentMetadataRequest;
import com.routeshare.driver.dto.response.DriverDocumentResponse;
import com.routeshare.driver.dto.response.DriverProfileResponse;
import com.routeshare.driver.entity.DriverDocumentEntity;
import com.routeshare.driver.entity.DriverProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = RouteShareMapperConfig.class)
public interface DriverMapper {
  DriverProfileResponse toProfileResponse(DriverProfileEntity entity);

  DriverDocumentResponse toDocumentResponse(DriverDocumentEntity entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "driverProfileId", source = "driverProfileId")
  @Mapping(target = "documentType", source = "request.documentType")
  @Mapping(target = "storageKey", source = "request.storageKey")
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "rejectionReason", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  DriverDocumentEntity toDocumentEntity(long driverProfileId, DocumentMetadataRequest request);
}
