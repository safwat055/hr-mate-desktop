package com.safwat.hr.service.payroll.dto;


import lombok.Data;

import java.time.LocalDate;


@Data
public class PayrollRequest {
    private String user;
    private String report;
    private String reportName;
    private String searchValue;
    private LocalDate startDate;
    private LocalDate endDate;

    private String nationalId;
    private String payGroup;
    private String management;
    private String customGroup;
    private String description;
    private String note;

    private Long reportId;
    private String fileName;
    private String format;
}