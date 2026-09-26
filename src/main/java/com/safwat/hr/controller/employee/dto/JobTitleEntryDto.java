package com.safwat.hr.controller.employee.dto;

import java.time.LocalDate;

public record JobTitleEntryDto(LocalDate effectiveFrom, Long jobTitleId, String jobTitleCode, String jobTitleNameAr) {
}