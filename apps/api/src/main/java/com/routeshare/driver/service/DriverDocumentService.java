package com.routeshare.driver.service;

import com.routeshare.driver.dto.response.DriverDocumentResponse;
import com.routeshare.storage.dto.DownloadUrlResponse;
import com.routeshare.storage.dto.UploadUrlRequest;
import com.routeshare.storage.dto.UploadUrlResponse;
import java.util.List;

public interface DriverDocumentService {
  /** Validates the request, reserves a document row, and returns a presigned upload URL. */
  UploadUrlResponse createUploadUrl(UploadUrlRequest req);

  /** Confirms the upload landed in storage and moves the document into review. */
  DriverDocumentResponse submit(long documentId);

  List<DriverDocumentResponse> listMine();

  /** Short-lived presigned download URL for the owning driver's own document. */
  DownloadUrlResponse downloadUrl(long documentId);
}
