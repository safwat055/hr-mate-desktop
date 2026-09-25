package com.safwat.hr.controller.employee.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record JobTitleEntryRequest(@NotNull LocalDate effectiveFrom, @NotNull Long jobTitleId) {
}