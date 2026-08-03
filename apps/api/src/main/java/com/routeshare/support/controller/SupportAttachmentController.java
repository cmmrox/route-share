package com.routeshare.support.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.support.dto.*;
import com.routeshare.support.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/support/tickets/{ticketId}/attachments")
@PreAuthorize("isAuthenticated()")
public class SupportAttachmentController {
  private final SupportService support;

  public SupportAttachmentController(SupportService support) {
    this.support = support;
  }

  @PostMapping("/upload-url")
  ApiResponse<SupportAttachmentUploadResponse> upload(
      @PathVariable long ticketId, @Valid @RequestBody SupportAttachmentUploadRequest request) {
    return ApiResponse.ok(support.createAttachmentUpload(ticketId, request));
  }

  @PostMapping("/{attachmentId}/submit")
  ApiResponse<SupportAttachmentResponse> submit(
      @PathVariable long ticketId, @PathVariable long attachmentId) {
    return ApiResponse.ok(support.submitAttachment(ticketId, attachmentId));
  }
}
