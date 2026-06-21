package com.routeshare.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.routeshare.admin.repository.AdminAnalyticsRepository;
import com.routeshare.admin.repository.AdminAnalyticsRepository.EntryTypeTotalRow;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminReportServiceImplTest {
  private final AdminAnalyticsRepository analytics = mock(AdminAnalyticsRepository.class);
  private final AdminReportServiceImpl service = new AdminReportServiceImpl(analytics);

  private static EntryTypeTotalRow row(String type, String amount) {
    return new EntryTypeTotalRow() {
      @Override
      public String getEntryType() {
        return type;
      }

      @Override
      public BigDecimal getAmount() {
        return new BigDecimal(amount);
      }
    };
  }

  @Test
  void financeReportAggregatesLedgerTotals() {
    when(analytics.financeTotalsBetween(any(), any()))
        .thenReturn(
            List.of(
                row("PAYMENT_CAPTURED", "1500.00"),
                row("PLATFORM_COMMISSION", "150.00"),
                row("PAYMENT_REFUNDED", "-50.00")));

    var report = service.report("finance", null, null);

    assertThat(report.reportType()).isEqualTo("FINANCE");
    assertThat(report.metrics())
        .anySatisfy(
            m -> {
              assertThat(m.key()).isEqualTo("grossCaptured");
              assertThat(m.value()).isEqualTo("1500.00");
            })
        .anySatisfy(
            m -> {
              assertThat(m.key()).isEqualTo("refunds");
              assertThat(m.value()).isEqualTo("50.00"); // abs value
            });
  }

  @Test
  void operationsReportUsesCounts() {
    when(analytics.bookingsCreatedBetween(any(), any())).thenReturn(12L);
    when(analytics.bookingsCompletedBetween(any(), any())).thenReturn(9L);
    when(analytics.tripsCompletedBetween(any(), any())).thenReturn(7L);
    when(analytics.newUsersBetween(any(), any())).thenReturn(3L);

    var report = service.report("OPERATIONS", null, null);

    assertThat(report.metrics())
        .extracting("key", "value")
        .contains(
            org.assertj.core.groups.Tuple.tuple("bookingsCreated", "12"),
            org.assertj.core.groups.Tuple.tuple("tripsCompleted", "7"));
  }

  @Test
  void csvExportHasHeaderAndRows() {
    when(analytics.bookingsCreatedBetween(any(), any())).thenReturn(1L);
    when(analytics.bookingsCompletedBetween(any(), any())).thenReturn(1L);
    when(analytics.tripsCompletedBetween(any(), any())).thenReturn(1L);
    when(analytics.newUsersBetween(any(), any())).thenReturn(1L);

    String csv = service.reportCsv("OPERATIONS", null, null);

    assertThat(csv).startsWith("report_type,from,to,metric_key,metric_label,value\n");
    assertThat(csv).contains("bookingsCreated");
  }

  @Test
  void rejectsUnknownReportType() {
    assertThatThrownBy(() -> service.report("UNKNOWN", null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsInvertedWindow() {
    Instant now = Instant.now();
    assertThatThrownBy(() -> service.report("FINANCE", now, now.minusSeconds(60)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
