package com.safwat.hr.service.payroll.dto;

import java.time.LocalDate;
import java.util.List;

public class DTO {
    public record ChangeCardView(
            String national_id,
            String pay_id,
            String emp_name,
            String management,
            LocalDate start_date,
            LocalDate end_date,
            List<Object[]> rows
    ) {
    }

    public record PaymentsView(
            String national_id, String pay_id, String emp_name, List<Object[]> rows, String[] headers
    ) {

    }

}
