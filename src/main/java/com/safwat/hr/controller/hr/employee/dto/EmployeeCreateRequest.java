package com.safwat.hr.controller.hr.employee.dto;

import java.time.LocalDate;

public record EmployeeCreateRequest(
        String employeeNumber,
        String fullName,
        String nationalId,
        LocalDate hireDate,
        Long sectorId,
        String management,                  // ← جديد
        LocalDate disabilityStartDate,
        String disabilityDecisionNo,
        boolean worksInRemoteGovernorate
) {
}