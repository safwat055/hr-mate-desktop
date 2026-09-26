package com.safwat.hr.controller.wages.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VariableWageDocumentCreateRequest(
        String nationalId,
        LocalDate periodMonth,
        String documentName,
        String documentNumber,
        BigDecimal totalAmount,
        Boolean subjectToPension,
        BigDecimal pensionSubjectAmount,
        Boolean subjectToTax,
        BigDecimal taxSubjectAmount,
        BigDecimal taxAmount,
        BigDecimal stampDutyAmount,
        String notes
) {
}