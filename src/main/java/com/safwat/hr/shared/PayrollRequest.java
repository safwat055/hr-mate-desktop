package com.safwat.hr.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PayrollRequest {
    private String user;
    private String report;
    private String reportType;
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
    private String endPoint;
    @Builder.Default
    private String format = "PDF";
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object payload;   // ← يقبل List, Map, Object, أي حاجة
}