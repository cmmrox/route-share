package com.routeshare.admin.service.impl;

import com.routeshare.admin.dto.AdminReportResponse;
import com.routeshare.admin.dto.AdminReportResponse.Metric;
import com.routeshare.admin.repository.AdminAnalyticsRepository;
import com.routeshare.admin.service.AdminReportService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {
  private static final String CURRENCY = "LKR";
  private static final String FINANCE = "FINANCE";
  private static final String OPERATIONS = "OPERATIONS";
  private static final Duration DEFAULT_WINDOW = Duration.ofDays(30);

  private final AdminAnalyticsRepository analytics;

  @Override
  @Transactional(readOnly = true)
  public AdminReportResponse report(String reportType, Instant from, Instant to) {
    String type = normalizeType(reportType);
    Instant end = to == null ? Instant.now() : to;
    Instant start = from == null ? end.minus(DEFAULT_WINDOW) : from;
    if (!start.isBefore(end)) {
      throw new IllegalArgumentException("Report 'from' must be before 'to'");
    }
    List<Metric> metrics =
        FINANCE.equals(type) ? financeMetrics(start, end) : operationsMetrics(start, end);
    return new AdminReportResponse(type, start, end, CURRENCY, metrics);
  }

  @Override
  @Transactional(readOnly = true)
  public String reportCsv(String reportType, Instant from, Instant to) {
    AdminReportResponse report = report(reportType, from, to);
    StringBuilder csv = new StringBuilder();
    csv.append("report_type,from,to,metric_key,metric_label,value\n");
    for (Metric m : report.metrics()) {
      csv.append(csvCell(report.reportType()))
          .append(',')
          .append(csvCell(report.from().toString()))
          .append(',')
          .append(csvCell(report.to().toString()))
          .append(',')
          .append(csvCell(m.key()))
          .append(',')
          .append(csvCell(m.label()))
          .append(',')
          .append(csvCell(m.value()))
          .append('\n');
    }
    return csv.toString();
  }

  private List<Metric> financeMetrics(Instant from, Instant to) {
    Map<String, BigDecimal> totals = new HashMap<>();
    for (var row : analytics.financeTotalsBetween(from, to)) {
      totals.put(row.getEntryType(), row.getAmount() == null ? BigDecimal.ZERO : row.getAmount());
    }
    List<Metric> metrics = new ArrayList<>();
    metrics.add(money("grossCaptured", "Card payments captured", totals, "PAYMENT_CAPTURED"));
    metrics.add(money("cashCollected", "Cash collected", totals, "CASH_COLLECTED"));
    metrics.add(money("platformCommission", "Platform commission", totals, "PLATFORM_COMMISSION"));
    metrics.add(money("driverEarnings", "Driver earnings", totals, "DRIVER_EARNING"));
    metrics.add(money("refunds", "Refunds", totals, "PAYMENT_REFUNDED"));
    metrics.add(money("fareFinalized", "Fares finalized", totals, "FARE_FINALIZED"));
    return metrics;
  }

  private List<Metric> operationsMetrics(Instant from, Instant to) {
    List<Metric> metrics = new ArrayList<>();
    metrics.add(
        count("bookingsCreated", "Bookings created", analytics.bookingsCreatedBetween(from, to)));
    metrics.add(
        count(
            "bookingsCompleted",
            "Bookings completed",
            analytics.bookingsCompletedBetween(from, to)));
    metrics.add(
        count("tripsCompleted", "Trips completed", analytics.tripsCompletedBetween(from, to)));
    metrics.add(count("newUsers", "New users", analytics.newUsersBetween(from, to)));
    return metrics;
  }

  private Metric money(String key, String label, Map<String, BigDecimal> totals, String entryType) {
    BigDecimal value = totals.getOrDefault(entryType, BigDecimal.ZERO).abs();
    return new Metric(key, label, value.toPlainString());
  }

  private Metric count(String key, String label, long value) {
    return new Metric(key, label, Long.toString(value));
  }

  private String normalizeType(String reportType) {
    String type = reportType == null ? FINANCE : reportType.trim().toUpperCase(Locale.ROOT);
    if (!FINANCE.equals(type) && !OPERATIONS.equals(type)) {
      throw new IllegalArgumentException("Unsupported report type: " + reportType);
    }
    return type;
  }

  private String csvCell(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return '"' + value.replace("\"", "\"\"") + '"';
    }
    return value;
  }
}
