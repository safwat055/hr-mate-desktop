package com.safwat.hr.report.payroll;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportContext {
    private final String user;
    private final String reportName;
    private final String startDate;
    private final String endDate;
    private final String management;
    private final String payGroup;
    private final String nationalId;
    private final String customGroup;
    private final String description;
    private final String note;
    private final String format;
    private final String searchValue;
}