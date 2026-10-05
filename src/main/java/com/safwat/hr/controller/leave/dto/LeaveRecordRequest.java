package com.safwat.hr.controller.leave.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LeaveRecordRequest {
    private String nationalId;
    private String employeeNumber;
    private String leaveTypeCode;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String notes;
}