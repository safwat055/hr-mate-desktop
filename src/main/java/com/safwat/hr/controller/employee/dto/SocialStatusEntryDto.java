package com.safwat.hr.controller.employee.dto;

import java.time.LocalDate;

public record SocialStatusEntryDto(LocalDate effectiveFrom, String code, String labelAr) {
}