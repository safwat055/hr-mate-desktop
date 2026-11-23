package com.safwat.hr.controller.hr.employee.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagementEntryDto(
        LocalDate effectiveFrom,
        String managementName
) {
}