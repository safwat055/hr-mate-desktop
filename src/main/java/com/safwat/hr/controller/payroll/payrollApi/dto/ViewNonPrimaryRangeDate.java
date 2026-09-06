package com.safwat.hr.controller.payroll.payrollApi.dto;

import java.util.List;

public record ViewNonPrimaryRangeDate(
        String nationalId,
        String name,
        String payId,
        String basic306,
        String department,
        String bank,
        String branch,
        String degree,
        List<String> allowanceHeaders,
        List<String> deductionHeaders,
        List<Object[]> allowanceRows,
        List<Object[]> deductionRows
) {
}