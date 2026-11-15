package com.safwat.hr.controller.hr.employee.dto;

import java.time.LocalDate;

public record JobTitleEntryDto(LocalDate effectiveFrom, Long jobTitleId, String jobTitleCode, String jobTitleNameAr) {
}