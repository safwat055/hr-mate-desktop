package com.safwat.hr.controller.employee.dto;

import com.safwat.hr.controller.employee.enums.SocialStatus;


import java.time.LocalDate;

public record SocialStatusEntryRequest( LocalDate effectiveFrom, SocialStatus status) {
}