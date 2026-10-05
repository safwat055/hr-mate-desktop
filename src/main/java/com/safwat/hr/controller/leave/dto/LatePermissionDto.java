package com.safwat.hr.controller.leave.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class LatePermissionDto {
    private Long id;
    private String nationalId;
    private String employeeName;
    private LocalDate date;
    private BigDecimal hours;
    private Integer leaveYear;
    private String notes;
}