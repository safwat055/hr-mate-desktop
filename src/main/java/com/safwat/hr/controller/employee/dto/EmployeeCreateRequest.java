package com.safwat.hr.controller.employee.dto;

import java.time.LocalDate;

public record EmployeeCreateRequest(
        String employeeNumber,
        String fullName,
        String nationalId,
        LocalDate hireDate,
        Long sectorId,

        // جديد — إعاقة
        LocalDate disabilityStartDate,
        String disabilityDecisionNo,

        // جديد — محافظة نائية
        boolean worksInRemoteGovernorate
) {
}