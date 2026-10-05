package com.safwat.hr.controller.employee.dto;

import com.safwat.hr.controller.employee.enums.TerminationReason;

import java.time.LocalDate;

public record EmployeeUpdateRequest(
        String fullName,
        String nationalId,
        LocalDate hireDate,
        LocalDate terminationDate,
        TerminationReason terminationReason,
        Long sectorId,

        // جديد — إعاقة
        LocalDate disabilityStartDate,
        String disabilityDecisionNo,

        // جديد — محافظة نائية
        boolean worksInRemoteGovernorate
) {
}