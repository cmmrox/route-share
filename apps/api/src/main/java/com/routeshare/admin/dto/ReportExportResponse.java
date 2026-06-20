package com.routeshare.admin.dto;

import java.time.Instant;

/**
 * Acknowledges an admin report-export request. Generation is performed asynchronously off the
 * audit/event trail.
 */
public record ReportExportResponse(
    String jobId, String reportType, String status, Instant requestedAt) {}
