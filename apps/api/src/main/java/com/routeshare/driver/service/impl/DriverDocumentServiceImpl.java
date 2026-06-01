package com.routeshare.driver.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.dto.request.DocumentMetadataRequest;
import com.routeshare.driver.dto.response.DriverDocumentResponse;
import com.routeshare.driver.mapper.DriverMapper;
import com.routeshare.driver.repository.DriverDocumentRepository;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.driver.service.DriverDocumentService;
import com.routeshare.identity.facade.IdentityFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DriverDocumentServiceImpl implements DriverDocumentService {
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final DriverProfileRepository drivers;
  private final DriverDocumentRepository documents;
  private final DriverMapper driverMapper;

  @Transactional
  public DriverDocumentResponse create(DocumentMetadataRequest req) {
    long driverProfileId = currentDriverProfileId();
    return driverMapper.toDocumentResponse(
        documents.save(driverMapper.toDocumentEntity(driverProfileId, req)));
  }

  public List<DriverDocumentResponse> listMine() {
    return documents.findByDriverProfileIdOrderByIdDesc(currentDriverProfileId()).stream()
        .map(driverMapper::toDocumentResponse)
        .toList();
  }

  private long currentDriverProfileId() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return drivers
        .findIdByAppUserId(app.appUserId())
        .orElseThrow(() -> new AccessDeniedException("Driver profile is required"));
  }
}
