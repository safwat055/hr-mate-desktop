package com.safwat.hr.controller.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EmployeeCreateRequest(
        @NotBlank String employeeNumber,
        @NotBlank String fullName,
        @NotBlank @Size(min = 14, max = 14) String nationalId,
        @NotNull LocalDate hireDate,
        @NotNull Long sectorId
) {
}