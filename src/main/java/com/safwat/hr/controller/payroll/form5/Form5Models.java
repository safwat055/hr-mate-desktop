package com.safwat.hr.controller.payroll.form5;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTOs الشاشة. أسماء الحقول لازم تطابق الـ JSON اللي راجع من الباك.
 */
public final class Form5Models {

    private Form5Models() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmployeeHit(String nationalId, String employeeNumber, String fullName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Info(
            String fullName,
            String nationalId,
            String insuranceNumber,
            LocalDate birthDate,
            String jobTitle,
            String establishmentNumber,
            LocalDate hireDate,
            LocalDate terminationDate,
            String region,
            String periodTypeName
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BasicRow(LocalDate date, BigDecimal basicSalary) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VariableRow(LocalDate date, BigDecimal averageVariableWage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            Info employeeInfo,
            List<BasicRow> basicRows,
            List<VariableRow> variableRows,
            BigDecimal totalVariableWage,
            BigDecimal totalSubscriptionWage
    ) {
    }

    public record ExportRequest(
            String nationalId,
            List<BasicRow> basicRows,
            List<VariableRow> variableRows
    ) {
    }
}