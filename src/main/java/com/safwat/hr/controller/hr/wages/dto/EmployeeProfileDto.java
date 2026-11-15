package com.safwat.hr.controller.hr.wages.dto;

public record EmployeeProfileDto(
        String nationalId,
        String employeeNumber,
        String fullName
) {
}