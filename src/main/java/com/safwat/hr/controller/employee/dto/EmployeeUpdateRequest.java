package com.safwat.hr.controller.employee.dto;

import com.safwat.hr.controller.employee.enums.TerminationReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * employeeNumber و sectorId غير قابلين للتعديل بعد إنشاء الموظف (افتراض - عدّله لو عندك سيناريو مختلف).
 */
public record EmployeeUpdateRequest(
        @NotBlank String fullName,
        @NotBlank @Size(min = 14, max = 14) String nationalId,
        @NotNull LocalDate hireDate,
        LocalDate terminationDate,
        TerminationReason terminationReason
) {
}