package com.safwat.hr.controller.payroll.payrollApi.dto;

import java.util.List;

public record ViewMainRecordForRangeDate(
        String nationalId,
        String empName,
        String payId,
        String basic30_6,
        String department,
        String bank,
        String branch,
        String degree,
        List<String> allowancesHeader,
        List<String> deductionsHeader,
        List<Object[]> allowancesValues,
        List<Object[]> deductionsValues
) {
}