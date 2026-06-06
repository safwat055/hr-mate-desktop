package com.safwat.hr.requestModel;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class PayrollReportRequest {

    private String user;
    private String report;
    private String reportName;

    private LocalDate startDate;
    private LocalDate endDate;

    private String nationalId;
    private String payGroup;
    private String description;
    private String note;

    private Long reportId;
    private String fileName;
    private String format;
}
