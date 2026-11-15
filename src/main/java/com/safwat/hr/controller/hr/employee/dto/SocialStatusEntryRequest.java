package com.safwat.hr.controller.hr.employee.dto;

import com.safwat.hr.controller.hr.employee.enums.SocialStatus;

import java.time.LocalDate;

public record SocialStatusEntryRequest(LocalDate effectiveFrom, SocialStatus status) {
}