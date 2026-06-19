package com.routeshare.admin.service;

import com.routeshare.support.dto.SupportMessageResponse;
import com.routeshare.support.dto.SupportTicketResponse;
import com.routeshare.support.dto.TicketMessageRequest;
import java.util.List;

public interface AdminSupportService {
  List<SupportTicketResponse> list(String status, int limit);

  SupportTicketResponse get(long ticketId);

  SupportTicketResponse updateStatus(long ticketId, String status);

  SupportMessageResponse reply(long ticketId, TicketMessageRequest req);
}
