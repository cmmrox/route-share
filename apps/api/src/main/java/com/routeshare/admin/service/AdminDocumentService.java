package com.routeshare.admin.service;

import com.routeshare.admin.dto.AdminDocReviewRequest;
import com.routeshare.admin.dto.AdminDocumentResponse;
import com.routeshare.storage.dto.DownloadUrlResponse;

/** Admin review + secure signed download for driver, vehicle, and passenger documents. */
public interface AdminDocumentService {
  AdminDocumentResponse reviewDriverDocument(long documentId, AdminDocReviewRequest req);

  AdminDocumentResponse reviewVehicleDocument(long documentId, AdminDocReviewRequest req);

  AdminDocumentResponse reviewPassengerDocument(long documentId, AdminDocReviewRequest req);

  DownloadUrlResponse driverDocumentDownloadUrl(long documentId);

  DownloadUrlResponse vehicleDocumentDownloadUrl(long documentId);

  DownloadUrlResponse passengerDocumentDownloadUrl(long documentId);
}
