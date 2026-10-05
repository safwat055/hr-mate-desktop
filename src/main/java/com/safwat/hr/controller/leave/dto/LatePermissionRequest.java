package com.safwat.hr.controller.leave.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class LatePermissionRequest {
    private String nationalId;
    private String employeeNumber;
    private LocalDate date;
    private BigDecimal hours;
    private String notes;
}