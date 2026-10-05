package com.safwat.hr.controller.leave.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LeaveRecordDto {
    private Long id;
    private String nationalId;
    private String employeeNumber;
    private String employeeName;
    private String leaveTypeCode;
    private String leaveTypeNameAr;
    private boolean leaveTypeOpenEnded;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer days;
    private Integer leaveYear;
    private String status;
    private String notes;
}