package com.routeshare.admin.controller;

import com.routeshare.admin.dto.AdminReportResponse;
import com.routeshare.admin.service.AdminReportService;
import com.routeshare.common.web.ApiResponse;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','OPS_ADMIN','FINANCE_ADMIN')")
public class AdminReportController {
  private final AdminReportService service;

  public AdminReportController(AdminReportService service) {
    this.service = service;
  }

  @GetMapping("/api/v1/admin/reports/{reportType}")
  ApiResponse<AdminReportResponse> report(
      @PathVariable String reportType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to) {
    return ApiResponse.ok(service.report(reportType, from, to));
  }

  @GetMapping(value = "/api/v1/admin/reports/{reportType}/export", produces = "text/csv")
  ResponseEntity<String> export(
      @PathVariable String reportType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to) {
    String csv = service.reportCsv(reportType, from, to);
    String filename = reportType.toLowerCase(java.util.Locale.ROOT) + "-report.csv";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csv);
  }
}
