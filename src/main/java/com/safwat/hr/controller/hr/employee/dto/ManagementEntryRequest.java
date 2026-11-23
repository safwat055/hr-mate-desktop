package com.safwat.hr.controller.hr.employee.dto;

import java.time.LocalDate;

public record ManagementEntryRequest(
        LocalDate effectiveFrom,
        String managementName
) {
}