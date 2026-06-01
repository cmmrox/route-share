package com.routeshare.driver.service;

import com.routeshare.driver.dto.request.DocumentMetadataRequest;
import com.routeshare.driver.dto.response.DriverDocumentResponse;
import java.util.List;

public interface DriverDocumentService {
  DriverDocumentResponse create(DocumentMetadataRequest req);

  List<DriverDocumentResponse> listMine();
}
