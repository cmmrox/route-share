package com.routeshare.driver.mapper;

import com.routeshare.common.mapper.RouteShareMapperConfig;
import com.routeshare.driver.dto.response.DriverDocumentResponse;
import com.routeshare.driver.dto.response.DriverProfileResponse;
import com.routeshare.driver.entity.DriverDocumentEntity;
import com.routeshare.driver.entity.DriverProfileEntity;
import org.mapstruct.Mapper;

@Mapper(config = RouteShareMapperConfig.class)
public interface DriverMapper {
  DriverProfileResponse toProfileResponse(DriverProfileEntity entity);

  DriverDocumentResponse toDocumentResponse(DriverDocumentEntity entity);
}
