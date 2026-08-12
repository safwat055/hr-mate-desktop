package com.safwat.hr.shared;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PayrollRequest {
    private String user;
    private String report;
    private String reportName;
    private String searchValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private String nationalId;
    private String payGroup;
    private List<String> payGroups;
    private String management;
    private String customGroup;
    private String description;
    private String note;
    private Long reportId;
    private String fileName;
    private String endPoint;
    @Builder.Default
    private String format = "PDF";
}