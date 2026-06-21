package com.routeshare.admin.dto;

import java.time.Instant;
import java.util.List;

/** A computed admin analytics report over a time window. */
public record AdminReportResponse(
    String reportType, Instant from, Instant to, String currency, List<Metric> metrics) {

  public record Metric(String key, String label, String value) {}
}
