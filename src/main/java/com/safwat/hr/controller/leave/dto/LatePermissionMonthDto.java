package com.safwat.hr.controller.leave.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class LatePermissionMonthDto {
    private String month;
    private BigDecimal totalHours;
    private int deductedDays;
    private BigDecimal leftoverHours;
}