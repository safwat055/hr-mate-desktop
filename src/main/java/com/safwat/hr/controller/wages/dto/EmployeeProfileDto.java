package com.safwat.hr.controller.wages.dto;

public record EmployeeProfileDto(
        String nationalId,
        String employeeNumber,
        String fullName
) {
}