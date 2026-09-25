package com.safwat.hr.controller.employee.dto;

import com.safwat.hr.controller.employee.enums.SocialStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SocialStatusEntryRequest(@NotNull LocalDate effectiveFrom, @NotNull SocialStatus status) {
}