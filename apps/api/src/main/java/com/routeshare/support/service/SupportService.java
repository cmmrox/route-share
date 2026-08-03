package com.routeshare.support.service;

import com.routeshare.support.dto.CreateTicketRequest;
import com.routeshare.support.dto.SupportMessageResponse;
import com.routeshare.support.dto.SupportTicketResponse;
import com.routeshare.support.dto.TicketMessageRequest;
import java.util.List;

public interface SupportService {
  SupportTicketResponse create(String ownerRole, CreateTicketRequest req);

  List<SupportTicketResponse> listMine();

  SupportTicketResponse getMine(long ticketId);

  SupportMessageResponse addMessage(String senderRole, long ticketId, TicketMessageRequest req);

  com.routeshare.support.dto.SupportAttachmentUploadResponse createAttachmentUpload(
      long ticketId, com.routeshare.support.dto.SupportAttachmentUploadRequest request);

  com.routeshare.support.dto.SupportAttachmentResponse submitAttachment(
      long ticketId, long attachmentId);
}
