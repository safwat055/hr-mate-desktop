package com.safwat.hr.controller.hr.employee.dto;

import java.time.LocalDate;

public record SocialStatusEntryDto(LocalDate effectiveFrom, String code, String labelAr) {
}