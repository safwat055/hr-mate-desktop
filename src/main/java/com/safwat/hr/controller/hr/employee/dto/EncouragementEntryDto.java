package com.safwat.hr.controller.hr.employee.dto;

import java.time.LocalDate;

/**
 * تشجيعية — عرض فقط، مصدرها ScaleDto.encouragements
 */
public record EncouragementEntryDto(
        LocalDate date,
        String decisionNumber
) {
}
