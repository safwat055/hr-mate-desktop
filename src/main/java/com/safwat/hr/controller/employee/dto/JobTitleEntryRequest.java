package com.safwat.hr.controller.employee.dto;


import java.time.LocalDate;

public record JobTitleEntryRequest( LocalDate effectiveFrom,  Long jobTitleId) {
}