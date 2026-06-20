package com.routeshare.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record ReportExportRequest(@NotBlank String reportType) {}
