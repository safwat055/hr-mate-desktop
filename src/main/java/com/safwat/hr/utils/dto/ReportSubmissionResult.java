package com.safwat.hr.utils.dto;

import lombok.Data;

@Data
public class ReportSubmissionResult {
    private boolean success;
    private Long reportId;
    private String message;
}