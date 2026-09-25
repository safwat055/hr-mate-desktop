package com.safwat.hr.controller.employee.dto;


import java.time.LocalDate;

public record EmployeeCreateRequest(
         String employeeNumber,
         String fullName,
         String nationalId,
        LocalDate hireDate,
         Long sectorId
) {
}