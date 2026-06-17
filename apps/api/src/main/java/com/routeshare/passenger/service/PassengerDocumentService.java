package com.routeshare.passenger.service;

import com.routeshare.passenger.dto.response.PassengerDocumentResponse;
import com.routeshare.storage.dto.DownloadUrlResponse;
import com.routeshare.storage.dto.UploadUrlRequest;
import com.routeshare.storage.dto.UploadUrlResponse;
import java.util.List;

public interface PassengerDocumentService {
  UploadUrlResponse createUploadUrl(UploadUrlRequest req);

  PassengerDocumentResponse submit(long documentId);

  List<PassengerDocumentResponse> listMine();

  DownloadUrlResponse downloadUrl(long documentId);
}
