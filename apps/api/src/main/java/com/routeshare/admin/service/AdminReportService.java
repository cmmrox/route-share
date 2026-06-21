package com.routeshare.admin.service;

import com.routeshare.admin.dto.AdminReportResponse;
import java.time.Instant;

public interface AdminReportService {
  /** Computes a report (FINANCE or OPERATIONS) over [from, to); null bounds default to last 30d. */
  AdminReportResponse report(String reportType, Instant from, Instant to);

  /** Renders the same report as CSV bytes for download. */
  String reportCsv(String reportType, Instant from, Instant to);
}
