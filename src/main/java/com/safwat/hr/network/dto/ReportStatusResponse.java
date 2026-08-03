package com.safwat.hr.network.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * نسخة Frontend من حالة التقرير.
 * التواريخ String عشان JavaFX TableView.
 */
@Data
public class ReportStatusResponse {
    private Long reportId;
    private String reportName;
    private String reportCode;
    private String status;
    private Integer progress;
    private String message;
    private String queueType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String submittedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String startedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String finishedTime;
    private String output;
    private String errorMessage;
}