package com.safwat.hr.payroll.payrollApi.dto;


import java.util.List;

public record PaymentsView(
        String national_id, String pay_id, String emp_name, List<Object[]> rows, String[] headers
) {

}