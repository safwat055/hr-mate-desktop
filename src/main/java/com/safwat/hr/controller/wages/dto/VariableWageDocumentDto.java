package com.safwat.hr.controller.wages.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VariableWageDocumentDto(
        Long id,
        String nationalId,
        String employeeNumber,
        String employeeName,
        LocalDate periodMonth,
        String documentName,
        String documentNumber,
        BigDecimal totalAmount,
        boolean subjectToPension,
        BigDecimal pensionSubjectAmount,
        boolean subjectToTax,
        BigDecimal taxSubjectAmount,
        BigDecimal taxAmount,
        BigDecimal stampDutyAmount,
        String notes
) {
}