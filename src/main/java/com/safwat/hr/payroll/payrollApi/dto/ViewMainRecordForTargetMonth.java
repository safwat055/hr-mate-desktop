package com.safwat.hr.payroll.payrollApi.dto;

import java.time.LocalDate;
import java.util.List;

public record ViewMainRecordForTargetMonth(
        LocalDate month,
        String nationalId,
        String empName,
        String payId,
        String basic30_6,
        String department,
        String bank,
        String branch,
        String degree,
        List<String> allowancesHeader,
        List<String> allowancesValues,
        List<String> deductionsHeader,
        List<String> deductionsValues
) {
}

