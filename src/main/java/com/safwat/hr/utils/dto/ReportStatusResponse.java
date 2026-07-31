package com.safwat.hr.utils.dto;

import lombok.Data;

@Data
public class ReportStatusResponse {
    private Long reportId;
    private String reportName;
    private String reportCode;
    private String status;      // PENDING / QUEUED / RUNNING / COMPLETED / FAILED
    private Integer progress;   // 0 - 100
    private String message;
    private String queueType;
    private String submittedTime;
    private String startedTime;
    private String finishedTime;
    private String output;      // مسار الملف أو الرسالة
    private String errorMessage;
}